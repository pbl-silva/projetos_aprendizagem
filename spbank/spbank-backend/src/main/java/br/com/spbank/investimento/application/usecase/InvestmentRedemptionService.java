package br.com.spbank.investimento.application.usecase;

import br.com.spbank.investimento.application.model.*;
import br.com.spbank.investimento.application.port.out.*;
import br.com.spbank.investimento.application.service.*;
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

@Service @Transactional
public class InvestmentRedemptionService implements br.com.spbank.investimento.application.port.in.InvestmentRedemptionUseCase {
    private final InvestmentPersistence positions;
    private final InvestmentRedemptions redemptions;
    private final AuthenticationPersistence customers;
    private final AccountPersistence accounts;
    private final AccountEntryPersistence entries;
    private final InvestmentBusinessCalendar calendar;
    private final InvestmentValuationCalculator calculator;
    private final Clock clock;

    public InvestmentRedemptionService(InvestmentPersistence positions, InvestmentRedemptions redemptions,
            AuthenticationPersistence customers, AccountPersistence accounts, AccountEntryPersistence entries,
            InvestmentBusinessCalendar calendar, InvestmentValuationCalculator calculator, Clock clock) {
        this.positions=positions; this.redemptions=redemptions; this.customers=customers; this.accounts=accounts;
        this.entries=entries; this.calendar=calendar; this.calculator=calculator; this.clock=clock;
    }


    @Transactional(readOnly=true)
    public List<InvestmentRedemption> history(UUID customer) { active(customer,false); return redemptions.list(customer); }

    @Transactional(readOnly=true)
    public Preview previewAmount(UUID customer,UUID id,BigDecimal amount){active(customer,false);return previewAmount(position(customer,id),amount);}

    public InvestmentRedemption requestAmount(UUID customer,UUID id,UUID key,BigDecimal amount,String password) {
        active(customer,true);
        if(customers.findCredentialByCustomerId(customer).filter(c->c.active()&&PasswordHasher.matches(password,c.passwordHash())).isEmpty())
            throw error("PASSWORD");
        amount=money(amount);
        var fingerprint=PasswordHasher.tokenHash("REDEEM_AMOUNT|"+id+"|"+amount.stripTrailingZeros().toPlainString());
        var old=redemptions.list(customer).stream().filter(r->r.key().equals(key)).findFirst();
        if(old.isPresent()) { var row=old.orElseThrow(); if(!row.fingerprint().equals(fingerprint))throw new IdempotencyConflictException(); return row; }
        if(positions.operation(customer,key).isPresent())throw new IdempotencyConflictException();
        var p=position(customer,id); var preview=previewAmount(p,amount);
        // A request made outside a business day waits for the next conversion day.
        boolean converted=preview.conversionDate().equals(calendar.today(clock));
        var row=new InvestmentRedemption(UUID.randomUUID(),customer,id,key,fingerprint,preview.units(),preview.estimatedAmount(),
                preview.conversionDate(),preview.paymentDate(),converted?"PENDING":"CONVERSION_PENDING",clock.instant(),null);
        redemptions.save(row); return row;
    }

    private Preview preview(InvestmentPosition p, BigDecimal requested) {
        if(p.product().type()!=InvestmentProduct.Type.FUND)throw error("LIQUIDITY");
        requested=normalize(requested);
        var rows=redemptions.list(p.customerId()).stream().filter(r->r.positionId().equals(p.id())&&!r.status().equals("FAILED")).toList();
        if(rows.stream().anyMatch(r->!r.status().equals("COMPLETED")))throw error("REDEMPTION_PENDING");
        var used=rows.stream().map(r->r.units()).reduce(BigDecimal.ZERO,(a,b)->a.add(b));
        var available=Objects.requireNonNull(p.units()).subtract(used);
        if(requested.compareTo(available)>0)throw error("UNITS_AVAILABLE");
        var date=calendar.nextOrSame(calendar.today(clock));
        return conversion(p,requested,available,date);
    }

    private Preview previewAmount(InvestmentPosition p,BigDecimal amount){
        if(p.product().type()!=InvestmentProduct.Type.FUND)throw error("LIQUIDITY");
        amount=money(amount);
        var used=redemptions.list(p.customerId()).stream().filter(r->r.positionId().equals(p.id())&&!r.status().equals("FAILED")&&!r.status().equals("CONVERSION_PENDING"))
                .map(r->r.units()).reduce(BigDecimal.ZERO,(a,b)->a.add(b));
        var available=Objects.requireNonNull(p.units()).subtract(used);
        return preview(p,amountUnits(p,amount,available,calendar.today(clock)));
    }

    private BigDecimal amountUnits(InvestmentPosition p,BigDecimal amount,BigDecimal available,LocalDate date){
        var quote=positions.quote(p.product().id(),date).orElseThrow(()->error("QUOTE"));
        var gross=available.multiply(quote.price()).setScale(2,RoundingMode.HALF_EVEN);
        if(amount.compareTo(gross)>0)throw error("AMOUNT_AVAILABLE");
        return amount.compareTo(gross)==0?available:amount.divide(quote.price(),12,RoundingMode.HALF_EVEN);
    }

    private Preview conversion(InvestmentPosition p, BigDecimal requested, BigDecimal available, LocalDate date) {
        var quote=positions.quote(p.product().id(),date.isAfter(calendar.today(clock))?calendar.today(clock):date).orElseThrow(()->error("QUOTE"));
        var remaining=available.subtract(requested);
        if(remaining.signum()>0&&remaining.multiply(quote.price()).compareTo(p.product().minimumRemaining())<0)throw error("REMAINING_MINIMUM");
        var amount=requested.multiply(quote.price()).setScale(2,RoundingMode.HALF_EVEN);
        if(amount.signum()<=0)throw error("AMOUNT");
        return new Preview(p.id(),requested,amount,remaining,date,calendar.nextOrSame(date.plusDays(1)));
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void process(UUID customer) {
        active(customer,true); var today=calendar.today(clock); if(!calendar.businessDay(today))return;
        var history=redemptions.list(customer);
        for(var p:positions.positions(customer)) {
            if(p.product().type()==InvestmentProduct.Type.CDB&&!Objects.requireNonNull(p.maturityDate()).isAfter(today)
                    &&history.stream().noneMatch(r->r.positionId().equals(p.id()))) {
                var value=calculator.calculate(p,today); if(value.grossValue()==null)continue;
                var row=new InvestmentRedemption(UUID.randomUUID(),customer,p.id(),p.id(),"MATURITY|"+p.id(),BigDecimal.ZERO,
                        Objects.requireNonNull(value.grossValue()),today,today,"PENDING",clock.instant(),null);
                redemptions.save(row); pay(p,row);
            }
        }
        for(var original:history) {
            var row=original;
            if(row.status().equals("CONVERSION_PENDING")&&!row.conversionDate().isAfter(today)) {
                var p=position(customer,row.positionId());
                var used=history.stream().filter(r->r.positionId().equals(p.id())&&r.status().equals("COMPLETED"))
                        .map(r->r.units()).reduce(BigDecimal.ZERO,(a,b)->a.add(b));
                Preview converted;
                try { var available=Objects.requireNonNull(p.units()).subtract(used);
                    var quantity=amountUnits(p,row.amount(),available,today);
                    converted=conversion(p,quantity,available,today); }
                catch(BusinessException failure) {
                    redemptions.save(new InvestmentRedemption(row.id(),customer,row.positionId(),row.key(),row.fingerprint(),row.units(),
                            BigDecimal.ZERO,today,today,"FAILED",row.requestedAt(),null)); continue;
                }
                row=new InvestmentRedemption(row.id(),customer,row.positionId(),row.key(),row.fingerprint(),converted.units(),
                        converted.estimatedAmount(),today,converted.paymentDate(),"PENDING",row.requestedAt(),null);
                redemptions.save(row);
            }
            if(row.status().equals("PENDING")&&!row.paymentDate().isAfter(today))pay(position(customer,row.positionId()),row);
        }
    }

    private void pay(InvestmentPosition p, InvestmentRedemption row) {
        var account=accounts.findAllForUpdate(List.of(p.accountId())).get(p.accountId());
        if(account==null||!account.isActive()||!account.getCustomerId().equals(p.customerId()))throw error("ACCOUNT");
        account.credit(row.amount()); accounts.saveAll(List.of(account));
        entries.saveAll(List.of(AccountEntry.create(account,row.id(),EntryReferenceType.INVESTMENT,EntryType.INVESTMENT_REDEMPTION,
                EntryDirection.CREDIT,row.amount(),"Resgate de "+p.product().name(),clock.instant())));
        redemptions.save(new InvestmentRedemption(row.id(),row.customerId(),row.positionId(),row.key(),row.fingerprint(),row.units(),
                row.amount(),row.conversionDate(),row.paymentDate(),"COMPLETED",row.requestedAt(),clock.instant()));
    }

    private InvestmentPosition position(UUID customer, UUID id) {
        return positions.positions(customer).stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->new NotFoundException("INVESTMENT_NOT_FOUND"));
    }
    private void active(UUID id, boolean lock) {
        (lock?customers.findCustomerByIdForUpdate(id):customers.findCustomerById(id)).filter(c->c.active()).orElseThrow(UnauthorizedException::new);
    }
    private static BigDecimal money(BigDecimal value){if(value==null||value.signum()<=0||value.scale()>2||value.precision()-value.scale()>17)throw error("AMOUNT");return value.setScale(2);}
    private static BigDecimal normalize(BigDecimal value) {
        if(value==null||value.signum()<=0||value.scale()>12||value.precision()>28)throw error("UNITS"); return value;
    }
    private static BusinessException error(String code) { return new BusinessException("INVESTMENT_"+code,"investment."+code.toLowerCase(Locale.ROOT)); }
}
