package br.com.spbank.investimento.application.usecase;
import br.com.spbank.investimento.application.model.*;
import br.com.spbank.investimento.application.port.in.InvestmentUseCase;
import br.com.spbank.investimento.application.port.out.InvestmentPersistence;
import br.com.spbank.investimento.application.service.InvestmentBusinessCalendar;
import br.com.spbank.investimento.application.service.InvestmentValuationCalculator;
import br.com.spbank.investimento.application.port.out.InvestmentReferenceData;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.conta.application.model.*;
import br.com.spbank.conta.application.port.out.*;
import br.com.spbank.shared.application.exception.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;
@Service
@Transactional
public class InvestmentService implements InvestmentUseCase {
    private static final String NOTICE="Laboratório: valores brutos, sem tributação ou suitability; não representa recomendação. Cota e CDI são simulados internamente.";
    private final InvestmentPersistence investments;
    private final AuthenticationPersistence customers;
    private final AccountPersistence accounts;
    private final AccountEntryPersistence entries;
    private final InvestmentBusinessCalendar calendar;
    private final InvestmentValuationCalculator valuation;
    private final InvestmentReferenceData reference;
    private final br.com.spbank.investimento.application.port.out.InvestmentRedemptions redemptions;
    private final Clock clock;
    private final JsonMapper json;
    public InvestmentService(InvestmentPersistence investments,AuthenticationPersistence customers,AccountPersistence accounts,
            AccountEntryPersistence entries,InvestmentBusinessCalendar calendar,Clock clock,JsonMapper json,InvestmentValuationCalculator valuation,InvestmentReferenceData reference,br.com.spbank.investimento.application.port.out.InvestmentRedemptions redemptions){
        this.investments=investments;this.customers=customers;this.accounts=accounts;this.entries=entries;this.calendar=calendar;this.clock=clock;this.json=json;this.valuation=valuation;this.reference=reference;this.redemptions=redemptions;
    }
    @Override @Transactional(readOnly=true)
    public List<ProductView> products(UUID customerId){customer(customerId,false);var today=calendar.today(clock);
        return investments.products().stream().filter(p->p.active()).map(p->new ProductView(p,p.fgcEligible(),
            p.type()==InvestmentProduct.Type.FUND?quote(p,today):null,NOTICE)).toList();}
    @Override @Transactional(readOnly=true)
    public Preview preview(UUID customerId,UUID accountId,UUID productId,BigDecimal amount){
        customer(customerId,false);var account=account(customerId,accountId,false);var product=product(productId,false);amount=amount(amount,product);
        account.requireAvailable(amount);var date=calendar.nextOrSame(calendar.today(clock));
        var quote=product.type()==InvestmentProduct.Type.FUND?quote(product,calendar.today(clock)):null;
        return new Preview(accountId,product,amount,date,quote,quote==null?null:units(amount,quote.price()),
            product.cdb()==null?null:date.plusDays(Objects.requireNonNull(product.cdb()).termDays()),true,NOTICE+" A prévia não reserva saldo nem garante a cota futura.");
    }
    @Override public InvestmentApplication apply(UUID customerId,UUID key,InvestmentCommand command){
        customer(customerId,true);password(customerId,command.confirmationPassword());Objects.requireNonNull(key,"Idempotency-Key");
        var value=money(command.amount());var fingerprint=PasswordHasher.tokenHash("APPLY|"+customerId+"|"+command.accountId()+"|"+command.productId()+"|"+value.toPlainString());
        if(redemptions.list(customerId).stream().anyMatch(r->r.key().equals(key)))throw new IdempotencyConflictException();
        var replay=investments.operation(customerId,key);
        if(replay.isPresent()){var old=replay.orElseThrow();if(!old.fingerprint().equals(fingerprint))throw new IdempotencyConflictException();return json.readValue(old.responseJson(),InvestmentApplication.class);}
        var product=product(command.productId(),true);value=amount(value,product);var account=account(customerId,command.accountId(),true);account.requireAvailable(value);
        var now=clock.instant();var date=calendar.nextOrSame(calendar.today(clock));
        var application=new InvestmentApplication(UUID.randomUUID(),customerId,account.getId(),product.id(),value,date,InvestmentApplication.Status.PENDING,now,null,null,null,null);
        investments.save(application);
        if(date.equals(calendar.today(clock)))application=complete(application,product,account,date);
        investments.save(new InvestmentOperation(UUID.randomUUID(),customerId,key,fingerprint,json.writeValueAsString(Objects.requireNonNull(application)),now));return application;
    }
    @Override @Transactional(readOnly=true) public List<InvestmentValuation> positions(UUID customerId){customer(customerId,false);var date=calendar.today(clock);return remaining(customerId).stream().map(position->valuation.calculate(position,date)).toList();}
    @Override @Transactional(propagation=Propagation.REQUIRES_NEW) public void processValuations(UUID customerId){
        customer(customerId,true);var date=calendar.today(clock);for(var position:remaining(customerId))reference.saveValuation(valuation.calculate(position,date));
    }
    @Override @Transactional(readOnly=true) public InvestmentApplication application(UUID customerId,UUID id){customer(customerId,false);return investments.application(customerId,id,false).orElseThrow(()->new NotFoundException("INVESTMENT_NOT_FOUND"));}
    @Override @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void process(UUID id){
        var owner=investments.owner(id);if(owner.isEmpty())return;var customerId=owner.orElseThrow();
        // All mutations use customer -> application/product -> account lock order.
        customers.findCustomerByIdForUpdate(customerId);
        var pending=investments.application(customerId,id,true).orElseThrow();var today=calendar.today(clock);
        if(pending.status()!=InvestmentApplication.Status.PENDING||pending.effectiveDate().isAfter(today)||!calendar.businessDay(today))return;
        InvestmentProduct product;Account account;
        try{customer(customerId,false);product=product(pending.productId(),true);amount(pending.amount(),product);
            account=account(customerId,pending.accountId(),true);account.requireAvailable(pending.amount());
            if(product.type()==InvestmentProduct.Type.FUND)quote(product,today);
        }catch(BusinessException failure){investments.save(new InvestmentApplication(pending.id(),customerId,pending.accountId(),pending.productId(),pending.amount(),pending.effectiveDate(),InvestmentApplication.Status.FAILED,pending.requestedAt(),clock.instant(),null,null,failure.getCode()));return;}
        // Technical exceptions after this point escape and roll back the entire transaction.
        complete(pending,product,account,today);
    }
    private @org.eclipse.jdt.annotation.NonNull InvestmentApplication complete(InvestmentApplication application,InvestmentProduct product,Account account,LocalDate date){
        var quote=product.type()==InvestmentProduct.Type.FUND?quote(product,date):null;
        var position=new InvestmentPosition(UUID.randomUUID(),application.id(),application.customerId(),account.getId(),product,application.amount(),date,
            quote==null?null:units(application.amount(),quote.price()),quote==null?null:quote.price(),quote==null?null:quote.date(),
            product.cdb()==null?null:date.plusDays(Objects.requireNonNull(product.cdb()).termDays()));
        account.debit(application.amount());accounts.saveAll(List.of(account));
        investments.save(position);
        entries.saveAll(List.of(AccountEntry.create(account,application.id(),EntryReferenceType.INVESTMENT,EntryType.INVESTMENT_APPLICATION,EntryDirection.DEBIT,application.amount(),"Aplicação em "+product.name(),clock.instant())));
        var completed=new InvestmentApplication(application.id(),application.customerId(),account.getId(),product.id(),application.amount(),date,InvestmentApplication.Status.COMPLETED,application.requestedAt(),clock.instant(),position.id(),account.getBalance(),null);
        investments.save(completed);return completed;
    }
    private List<InvestmentPosition> remaining(UUID customerId){
        var history=redemptions.list(customerId);var result=new ArrayList<InvestmentPosition>();
        for(var p:investments.positions(customerId)){
            var rows=history.stream().filter(r->r.positionId().equals(p.id())&&!r.status().equals("FAILED")&&!r.status().equals("CONVERSION_PENDING")).toList();
            if(p.product().type()==InvestmentProduct.Type.CDB){if(rows.isEmpty())result.add(p);continue;}
            var original=Objects.requireNonNull(p.units());var units=original.subtract(rows.stream().map(r->r.units()).reduce(BigDecimal.ZERO,(a,b)->a.add(b)));
            if(units.signum()>0){var principal=p.principal().multiply(units).divide(original,2,RoundingMode.HALF_EVEN);
                if(principal.signum()>0)result.add(new InvestmentPosition(p.id(),p.applicationId(),p.customerId(),p.accountId(),p.product(),principal,p.effectiveDate(),units,p.unitPrice(),p.quoteDate(),p.maturityDate()));}
        }return List.copyOf(result);
    }
    private void customer(UUID id,boolean lock){var found=lock?customers.findCustomerByIdForUpdate(id):customers.findCustomerById(id);found.filter(c->c.active()).orElseThrow(UnauthorizedException::new);}
    private void password(UUID id,String password){if(customers.findCredentialByCustomerId(id).filter(c->c.active()&&PasswordHasher.matches(password,c.passwordHash())).isEmpty())throw error("PASSWORD");}
    private InvestmentProduct product(UUID id,boolean lock){return investments.product(id,lock).filter(p->p.active()).orElseThrow(()->error("PRODUCT"));}
    private Account account(UUID customerId,UUID id,boolean lock){var account=lock?accounts.findAllForUpdate(List.of(id)).get(id):accounts.findById(id).orElse(null);
        if(account==null||!account.getCustomerId().equals(customerId)||!account.isActive())throw error("ACCOUNT");return account;}
    private InvestmentQuote quote(InvestmentProduct product,LocalDate date){return investments.quote(product.id(),date).orElseThrow(()->error("QUOTE"));}
    private static BigDecimal units(BigDecimal amount,BigDecimal price){return amount.divide(price,12,RoundingMode.HALF_EVEN);}
    private static BigDecimal amount(BigDecimal value,InvestmentProduct product){value=money(value);if(value.compareTo(product.minimumApplication())<0)throw error("MINIMUM");return value;}
    private static BigDecimal money(BigDecimal value){if(value==null||value.signum()<=0||value.scale()>2||value.precision()-value.scale()>17)throw error("AMOUNT");return value.setScale(2,RoundingMode.UNNECESSARY);}
    private static BusinessException error(String suffix){return new BusinessException("INVESTMENT_"+suffix,"investment."+suffix.toLowerCase(Locale.ROOT));}
}
