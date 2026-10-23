package br.com.spbank.cartao.application.usecase;

import static br.com.spbank.cartao.application.service.CardFinancePolicy.*;
import static br.com.spbank.cartao.application.model.finance.CardFinanceState.put;
import br.com.spbank.autenticacao.application.model.Customer;
import br.com.spbank.autenticacao.application.port.out.AuthenticationPersistence;
import br.com.spbank.autenticacao.application.service.PasswordHasher;
import br.com.spbank.administracao.application.port.out.AdministrationPersistence;
import br.com.spbank.cartao.application.exception.CardPinException;
import br.com.spbank.cartao.application.model.*;
import br.com.spbank.cartao.application.model.finance.*;
import br.com.spbank.cartao.application.port.in.CardInstrumentUseCase;
import br.com.spbank.cartao.application.port.out.*;
import br.com.spbank.cartao.application.service.*;
import br.com.spbank.conta.application.model.*;
import br.com.spbank.conta.application.port.out.*;
import br.com.spbank.shared.application.exception.*;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Financial writes serialize on the customer before loading the card ledger or locking accounts. */
@Service
@Transactional(noRollbackFor=CardPinException.class)
public class CardFinanceService {
    private final CardFinancePersistence persistence;
    private final AuthenticationPersistence customers;
    private final AdministrationPersistence administrators;
    private final CardInstrumentPersistence instruments;
    private final CardInstrumentUseCase instrumentUseCase;
    private final CardCredentialVault vault;
    private final CardCreditAllocationPersistence lines;
    private final AccountPersistence accounts;
    private final AccountEntryPersistence entries;
    private final CardBillingCalendar calendar;
    private final Clock clock;
    private final JsonMapper json;

    public CardFinanceService(CardFinancePersistence persistence,AuthenticationPersistence customers,
            AdministrationPersistence administrators,CardInstrumentPersistence instruments,
            CardInstrumentUseCase instrumentUseCase,CardCredentialVault vault,CardCreditAllocationPersistence lines,
            AccountPersistence accounts,AccountEntryPersistence entries,CardBillingCalendar calendar,Clock clock,JsonMapper json){
        this.persistence=persistence;this.customers=customers;this.administrators=administrators;this.instruments=instruments;
        this.instrumentUseCase=instrumentUseCase;this.vault=vault;this.lines=lines;this.accounts=accounts;this.entries=entries;
        this.calendar=calendar;this.clock=clock;this.json=json;
    }

    public CardSummary issue(UUID customerId,CardBrand brand,int dueDay){
        var customer=lock(customerId);validDay(dueDay);
        var state=load(customer);var existing=billing(state,brand);
        if(existing!=null&&calendar.day(existing,calendar.reference(existing,clock.instant()))!=dueDay)throw error("DUE_DAY");
        var result=instrumentUseCase.issue(customerId,brand);
        if(existing==null)put(state.billingLines,newLine(customerId,brand,dueDay));
        synchronize(state,customer);save(state);return result;
    }

    public CardFinanceState overview(UUID customerId){var customer=lock(customerId);var state=load(customer);save(state);return state;}
    public List<UUID> customerIds(){return persistence.customerIds();}
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processCustomer(UUID customerId){var customer=lock(customerId);save(load(customer));}

    public CardPurchasePreview preview(UUID customerId,CardPurchaseCommand command){
        var customer=lock(customerId);var state=load(customer);
        var card=findCard(customerId,command.pan());card=java.util.Objects.requireNonNull(validateCredentials(card,command));
        requirePurchasable(customer,state,card);return purchaseTerms(command,customer);
    }

    public CardFinanceResult purchase(UUID customerId,UUID key,CardPurchaseCommand command){
        var customer=lock(customerId);var state=load(customer);var amount=positive(command.amount());
        var card=findCard(customerId,command.pan());
        var hash=hash("PURCHASE",customerId,card==null?"UNKNOWN":card.id(),text(command.merchant()),amount,command.installments());
        var replay=replay(state,key,hash);
        if(replay!=null){if(card!=null)instrumentUseCase.details(customerId,card.id(),command.pin());return replay;}
        CardPurchasePreview preview;
        try{
            card=Objects.requireNonNull(validateCredentials(card,command));
            instrumentUseCase.details(customerId,card.id(),command.pin());
            requirePurchasable(customer,state,card);preview=purchaseTerms(command,customer);
            if(line(customerId,card.brand()).available().compareTo(preview.total())<0)throw error("LIMIT");
        }catch(BusinessException ex){
            var attempt=new CardAttempt(UUID.randomUUID(),customerId,card==null?null:card.brand(),card==null?null:card.id(),
                card==null?"****":card.lastFour(),text(command.merchant()),amount,"DECLINED",ex.getCode(),clock.instant());
            put(state.cardAttempts,attempt);
            return finish(state,key,hash,result("PURCHASE","DECLINED",attempt.id(),ZERO,ZERO,ZERO,ex.getCode()));
        }
        var purchase=new CardPurchase(UUID.randomUUID(),customerId,card.brand(),card.id(),text(command.merchant()),amount,
            preview.interest(),ZERO,ZERO,ZERO,customer.plan(),command.installments(),clock.instant());put(state.cardPurchases,purchase);
        var config=requiredBilling(state,card.brand());var reference=calendar.reference(config,clock.instant());
        for(int index=0;index<preview.installments().size();index++){
            var part=preview.installments().get(index);put(state.cardObligations,obligation(customerId,card.brand(),"PURCHASE",purchase.id(),
                index+1,reference.plusMonths(index),config,part.principal(),part.interest()));
        }
        put(state.cardAttempts,new CardAttempt(UUID.randomUUID(),customerId,card.brand(),card.id(),card.lastFour(),purchase.merchant(),amount,"APPROVED","APPROVED",clock.instant()));
        synchronize(state,customer);
        return finish(state,key,hash,result("PURCHASE","APPROVED",purchase.id(),preview.total(),ZERO,ZERO,"APPROVED"));
    }

    private CardPurchasePreview purchaseTerms(CardPurchaseCommand command,Customer customer){
        var principal=positive(command.amount());text(command.merchant());
        var interest=interest(principal,command.installments(),customer.plan(),false);
        return new CardPurchasePreview(principal,interest,principal.add(interest),split(principal,interest,command.installments(),new BigDecimal("10.00")));
    }
    private CardInstrument findCard(UUID customerId,String pan){
        if(pan==null||!pan.matches("[0-9]{16}"))return null;
        for(var card:instruments.cards(customerId))if(vault.reveal(card).pan().equals(pan))return card;
        return null;
    }
    private CardInstrument validateCredentials(CardInstrument card,CardPurchaseCommand command){
        if(card==null)throw error("CREDENTIALS");var details=vault.reveal(card);
        if(!details.cvv().equals(command.cvv())||!YearMonth.from(details.expiresOn()).toString().equals(command.expiry()))throw error("CREDENTIALS");
        return card;
    }
    private void requirePurchasable(Customer customer,CardFinanceState state,CardInstrument card){
        if(card.stateAt(today())!=CardState.ACTIVE||card.pinBlocked()||card.voluntaryBlocked()||card.administrativeBlocked())throw error("BLOCKED");
        BigDecimal allocated=lines.lines(customer.id()).stream().map((CardCreditLine value) -> java.util.Objects.requireNonNull(value).allocated()).reduce(ZERO,(left, right) -> java.util.Objects.requireNonNull(left).add(java.util.Objects.requireNonNull(right)));
        if(allocated.compareTo(CardCreditPolicy.ceiling(customer.plan()))>0)throw error("ADJUSTMENT");
        if(overdue(state,card.brand()))throw error("OVERDUE");
    }

    public CardFinanceResult requestRefund(UUID customerId,UUID key,UUID purchaseId,BigDecimal principal,String reason,String password){
        var customer=lock(customerId);password(customerId,password);var state=load(customer);principal=positive(principal);reason=text(reason);
        var hash=hash("REFUND_REQUEST",customerId,purchaseId,principal,reason);var replay=replay(state,key,hash);if(replay!=null)return replay;
        var purchase=find(state.cardPurchases,purchaseId);
        if(principal.compareTo(purchase.principal().subtract(purchase.refundedPrincipal()))>0)throw error("REFUND");
        var request=new CardRefundRequest(UUID.randomUUID(),customerId,purchase.brand(),purchaseId,principal,reason,"REQUESTED",null,null,clock.instant());
        put(state.cardRefundRequests,request);return finish(state,key,hash,result("REFUND_REQUEST","REQUESTED",request.id(),principal,ZERO,ZERO,"REQUESTED"));
    }

    public CardFinanceResult decideRefund(UUID adminId,UUID customerId,UUID key,UUID requestId,boolean approved,String reason){
        administrator(adminId);var customer=lock(customerId);var state=load(customer);reason=text(reason);
        var hash=hash("REFUND_DECISION",adminId,requestId,approved,reason);var replay=replay(state,key,hash);if(replay!=null)return replay;
        var request=find(state.cardRefundRequests,requestId);if(!request.status().equals("REQUESTED"))throw error("STATE");
        request=request.withStatus(approved?"APPROVED":"REJECTED").withAdministratorId(adminId).withDecisionReason(reason);
        put(state.cardRefundRequests,request);
        if(!approved)return finish(state,key,hash,result("REFUND_DECISION","REJECTED",requestId,ZERO,ZERO,ZERO,"REJECTED"));
        var purchase=find(state.cardPurchases,request.purchaseId());var accumulated=purchase.refundedPrincipal().add(request.principal());
        if(accumulated.compareTo(purchase.principal())>0)throw error("REFUND");
        var target=accumulated.compareTo(purchase.principal())==0?purchase.interest():round(purchase.interest().multiply(accumulated).divide(purchase.principal(),12,RoundingMode.HALF_UP));
        var interest=target.subtract(purchase.refundedInterest()).max(ZERO)
            .min(purchase.interest().subtract(purchase.discountedInterest()).subtract(purchase.refundedInterest()).max(ZERO));
        BigDecimal principalLeft=request.principal(),interestLeft=interest;
        var targets=state.cardObligations.stream().filter(o->o.sourceType().equals("PURCHASE")&&o.sourceId().equals(purchase.id()))
            .sorted(Comparator.comparingInt((CardObligation value) -> java.util.Objects.requireNonNull(value).sequence()).reversed()).toList();
        var financed=new LinkedHashSet<UUID>();
        for(var obligation:targets){
            if(obligation.financedAgreementId()!=null)financed.add(obligation.financedAgreementId());
            var p=principalLeft.min(obligation.remainingPrincipal());var i=interestLeft.min(obligation.remainingInterest());
            principalLeft=principalLeft.subtract(p);interestLeft=interestLeft.subtract(i);
            if(p.add(i).signum()>0)settle(state,obligation,requestId,"REFUND",p,i,ZERO);
        }
        BigDecimal left=principalLeft.add(interestLeft);
        for(var agreementId:financed)left=refundFinanced(state,agreementId,left,requestId,new HashSet<>());
        var config=requiredBilling(state,purchase.brand());put(state.billingLines,config.withCreditBalance(config.creditBalance().add(left)));
        put(state.cardPurchases,purchase.withRefundedPrincipal(accumulated).withRefundedInterest(purchase.refundedInterest().add(interest)));
        var refund=new CardRefund(UUID.randomUUID(),customerId,purchase.brand(),requestId,purchase.id(),request.principal(),interest,left,clock.instant());put(state.cardRefunds,refund);
        reconcileInternal(state,purchase.brand(),requestId);
        return finish(state,key,hash,result("REFUND_DECISION","APPROVED",refund.id(),request.principal().add(interest),ZERO,left,"APPROVED"));
    }

    private BigDecimal refundFinanced(CardFinanceState state,UUID agreementId,BigDecimal left,UUID operation,Set<UUID> visited){
        if(!visited.add(agreementId))return left;
        var rows=state.cardObligations.stream().filter(o->o.sourceType().equals("AGREEMENT")&&o.sourceId().equals(agreementId))
            .sorted(Comparator.comparingInt((CardObligation value) -> java.util.Objects.requireNonNull(value).sequence()).reversed()).toList();
        for(var row:rows){if(left.signum()==0)break;
            if(row.financedAgreementId()!=null){left=refundFinanced(state,row.financedAgreementId(),left,operation,visited);continue;}
            var amount=left.min(row.remainingPrincipal());settle(state,row,operation,"REFUND",amount,ZERO,ZERO);left=left.subtract(amount);
        }return left;
    }

    public record InvoiceView(CardInvoice invoice,List<CardInvoiceItem> items,List<CardObligation> obligations,List<CardLateEpisode> charges,
            List<CardSettlement> allocations,BigDecimal remaining,String message){}
    public InvoiceView invoice(UUID customerId,UUID invoiceId){var state=overview(customerId);return invoiceView(state,find(state.cardInvoices,invoiceId));}
    public InvoiceView invoiceView(CardFinanceState state,CardInvoice invoice){
        var obligations=state.cardObligations.stream().filter(o->o.brand()==invoice.brand()&&o.reference().equals(invoice.reference())).toList();
        var ids=obligations.stream().map((CardObligation value) -> java.util.Objects.requireNonNull(value).id()).toList();
        var charges=state.cardLateEpisodes.stream().filter(e->ids.contains(e.obligationId())).toList();
        var remaining=obligations.stream().map(this::remaining).reduce(ZERO,(left, right) -> java.util.Objects.requireNonNull(left).add(java.util.Objects.requireNonNull(right)))
            .add(charges.stream().map(e->e.remainingFine().add(e.remainingMora())).reduce(ZERO,(left, right) -> java.util.Objects.requireNonNull(left).add(java.util.Objects.requireNonNull(right))));
        return new InvoiceView(invoice,state.cardInvoiceItems.stream().filter(i->i.invoiceId().equals(invoice.id())).toList(),obligations,charges,
            state.cardSettlements.stream().filter(a->ids.contains(a.obligationId())).toList(),remaining,remaining.signum()==0?"Sem valor a pagar":"Valor a pagar");
    }
    public CardFinanceResult changeDueDay(UUID customerId,UUID key,CardBrand brand,int day,String password){
        var customer=lock(customerId);password(customerId,password);validDay(day);var state=load(customer);
        var hash=hash("DUE_DAY",customerId,brand,day);var replay=replay(state,key,hash);if(replay!=null)return replay;
        var config=requiredBilling(state,brand);var current=calendar.reference(config,clock.instant());var effective=current.plusMonths(1);
        var updated=config.withDueDay(calendar.day(config,current)).withPendingDueDay(day).withEffectiveReference(effective.toString());
        put(state.billingLines,updated);
        for(var row:List.copyOf(state.cardObligations))if(row.brand()==brand&&row.status().equals("PENDING")&&row.reference().compareTo(effective.toString())>=0){
            var nominal=calendar.nominal(updated,YearMonth.parse(row.reference()));
            put(state.cardObligations,row.withNominalDue(nominal).withEffectiveDue(calendar.effective(nominal)).withClosingAt(calendar.closing(updated,YearMonth.parse(row.reference()))));
        }
        var change=new CardDueDayChange(UUID.randomUUID(),customerId,brand,calendar.day(config,current),day,effective.toString(),clock.instant());put(state.cardDueDayChanges,change);
        return finish(state,key,hash,result("DUE_DAY","SCHEDULED",change.id(),ZERO,ZERO,ZERO,effective.toString()));
    }
    public CardFinanceResult preference(UUID customerId,UUID key,UUID accountId,String password){
        var customer=lock(customerId);password(customerId,password);var state=load(customer);
        var hash=hash("PREFERENCE",customerId,accountId);var replay=replay(state,key,hash);if(replay!=null)return replay;
        ownedAccount(customerId,accountId,false);put(state.cardPaymentPreferences,new CardPaymentPreference(customerId,customerId,accountId));
        return finish(state,key,hash,result("PREFERENCE","SAVED",accountId,ZERO,ZERO,ZERO,"SAVED"));
    }
    public CardFinanceResult payAccount(UUID customerId,UUID key,CardBrand brand,BigDecimal amount,UUID accountId,String password){
        var customer=lock(customerId);password(customerId,password);var state=load(customer);amount=positive(amount);
        var chosen=chosenAccount(state,accountId);var hash=hash("ACCOUNT_PAYMENT",customerId,brand,amount,chosen);var replay=replay(state,key,hash);if(replay!=null)return replay;
        if(requiredBilling(state,brand).creditBalance().signum()>0)throw error("CREDIT_RECONCILIATION");
        if(amount.compareTo(payable(state,brand))>0)throw error("PAYMENT");
        var id=UUID.randomUUID();debit(customerId,chosen,amount,id,"Pagamento de fatura de cartão");
        var credit=allocate(state,brand,amount,id,"PAYMENT",false);
        put(state.cardPayments,payment(id,customerId,brand,"INVOICE",null,"ACCOUNT",amount,credit,ZERO,"CONFIRMED",chosen,null,null,null,clock.instant()));
        return finish(state,key,hash,result("ACCOUNT_PAYMENT","CONFIRMED",id,amount,ZERO,credit,"CONFIRMED"));
    }
    public CardFinanceResult boleto(UUID customerId,UUID key,CardBrand brand,BigDecimal amount,UUID proposalId,String password){
        var customer=lock(customerId);password(customerId,password);var state=load(customer);amount=positive(amount);
        var hash=hash("BOLETO",customerId,brand,amount,proposalId);var replay=replay(state,key,hash);if(replay!=null)return replay;
        if(proposalId==null){if(amount.compareTo(payable(state,brand))>0)throw error("PAYMENT");}
        else{var proposal=find(state.cardProposals,proposalId);if(proposal.brand()!=brand||amount.compareTo(proposal.entry())!=0||!proposal.status().equals("PROPOSED"))throw error("PROPOSAL");}
        var id=UUID.randomUUID();var code=java.util.stream.IntStream.range(0,44).mapToObj(i->Integer.toString(new java.security.SecureRandom().nextInt(10))).collect(java.util.stream.Collectors.joining());
        put(state.cardPayments,payment(id,customerId,brand,proposalId==null?"INVOICE":"AGREEMENT_ENTRY",proposalId,"BOLETO",amount,ZERO,ZERO,"PENDING",null,null,null,code,null));
        return finish(state,key,hash,result("BOLETO","PENDING",id,amount,ZERO,ZERO,"PENDING"));
    }
    public CardFinanceResult confirmBoleto(UUID adminId,UUID customerId,UUID key,UUID paymentId,String reason){
        administrator(adminId);var customer=lock(customerId);var state=load(customer);reason=text(reason);
        var hash=hash("BOLETO_CONFIRM",adminId,paymentId,reason);var replay=replay(state,key,hash);if(replay!=null)return replay;
        var payment=find(state.cardPayments,paymentId);if(!payment.status().equals("PENDING"))throw error("STATE");
        BigDecimal credit;
        if(payment.proposalId()!=null)credit=activateProposal(state,customer,find(state.cardProposals,payment.proposalId()),payment.amount(),paymentId);
        else credit=allocate(state,payment.brand(),payment.amount(),paymentId,"PAYMENT",true);
        put(state.cardPayments,payment.withStatus("CONFIRMED").withPaidAt(clock.instant()).withAppliedAmount(payment.amount().subtract(credit))
            .withCreditedAmount(credit).withAdministratorId(adminId).withReason(reason));
        return finish(state,key,hash,result("BOLETO_CONFIRM","CONFIRMED",paymentId,payment.amount(),ZERO,credit,"CONFIRMED"));
    }

    public record AnticipationPreview(List<UUID> obligations,BigDecimal originalAmount,BigDecimal discount,BigDecimal amount){}
    public AnticipationPreview anticipation(UUID customerId,CardBrand brand,UUID sourceId,String sourceType,int count){
        var state=load(lock(customerId));return anticipationTerms(state,brand,sourceId,sourceType,count);
    }
    private AnticipationPreview anticipationTerms(CardFinanceState state,CardBrand brand,UUID sourceId,String sourceType,int count){
        if(overdue(state,brand))throw error("OVERDUE");if(count<1)throw error("INSTALLMENTS");
        var selected=state.cardObligations.stream().filter(o->o.brand()==brand&&o.sourceType().equals(sourceType)&&o.sourceId().equals(sourceId)
            &&remaining(o).signum()>0&&o.effectiveDue().isAfter(today())).sorted(Comparator.comparing((CardObligation value) -> java.util.Objects.requireNonNull(value).effectiveDue()).thenComparingInt((CardObligation value) -> java.util.Objects.requireNonNull(value).sequence())).limit(count).toList();
        if(selected.size()!=count)throw error("INSTALLMENTS");BigDecimal total=ZERO,discount=ZERO;
        BigDecimal rate=sourceType.equals("PURCHASE")?purchaseRate(find(state.cardPurchases,sourceId).plan()):find(state.cardAgreements,sourceId).rate();
        for(var row:selected){total=total.add(remaining(row));discount=discount.add(discount(row,rate));}
        return new AnticipationPreview(selected.stream().map((CardObligation value) -> java.util.Objects.requireNonNull(value).id()).toList(),total,discount,total.subtract(discount));
    }
    public CardFinanceResult anticipate(UUID customerId,UUID key,CardBrand brand,UUID sourceId,String sourceType,int count,
            BigDecimal expectedAmount,UUID accountId,String password){
        var customer=lock(customerId);password(customerId,password);var state=load(customer);var chosen=chosenAccount(state,accountId);
        var hash=hash("ANTICIPATION",customerId,brand,sourceId,sourceType,count,positive(expectedAmount),chosen);var replay=replay(state,key,hash);if(replay!=null)return replay;
        var preview=anticipationTerms(state,brand,sourceId,sourceType,count);if(preview.amount().compareTo(expectedAmount)!=0)throw error("STALE");
        if(requiredBilling(state,brand).creditBalance().signum()>0)throw error("CREDIT_RECONCILIATION");
        var id=UUID.randomUUID();debit(customerId,chosen,preview.amount(),id,"Antecipação de parcelas de cartão");
        var rate=sourceType.equals("PURCHASE")?purchaseRate(find(state.cardPurchases,sourceId).plan()):find(state.cardAgreements,sourceId).rate();
        for(var obligationId:preview.obligations()){
            var row=find(state.cardObligations,obligationId);var discount=discount(row,rate);
            settle(state,row,id,"ANTICIPATED",row.remainingPrincipal(),row.remainingInterest().subtract(discount),discount);
            if(sourceType.equals("PURCHASE")){var purchase=find(state.cardPurchases,sourceId);put(state.cardPurchases,purchase.withDiscountedInterest(purchase.discountedInterest().add(discount)));}
        }
        put(state.cardPayments,payment(id,customerId,brand,"ANTICIPATION",null,"ACCOUNT",preview.amount(),ZERO,preview.discount(),"CONFIRMED",chosen,null,null,null,clock.instant()));
        closeAgreements(state);
        return finish(state,key,hash,result("ANTICIPATION","CONFIRMED",id,preview.amount(),preview.discount(),ZERO,"CONFIRMED"));
    }
    private BigDecimal discount(CardObligation row,BigDecimal rate){return round(row.remainingPrincipal().multiply(rate)
        .multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(today(),row.effectiveDue()))).divide(new BigDecimal("30"),12,RoundingMode.HALF_UP)).min(row.remainingInterest());}

    public CardFinanceResult propose(UUID customerId,UUID key,CardBrand brand,List<UUID> invoiceIds,UUID previousAgreementId,int count,String password){
        var customer=lock(customerId);password(customerId,password);var state=load(customer);
        var normalized=invoiceIds==null?List.<UUID>of():invoiceIds.stream().distinct().sorted().toList();
        var hash=hash("PROPOSE",customerId,brand,normalized,previousAgreementId,count);var replay=replay(state,key,hash);if(replay!=null)return replay;
        var covered=new LinkedHashSet<UUID>();
        for(var invoiceId:normalized){var invoice=find(state.cardInvoices,invoiceId);if(invoice.brand()!=brand)throw error("OWNERSHIP");
            state.cardObligations.stream().filter(o->o.brand()==brand&&o.reference().equals(invoice.reference())&&owed(state,o).signum()>0).map((CardObligation value) -> java.util.Objects.requireNonNull(value).id()).forEach(covered::add);}
        if(previousAgreementId!=null){var old=find(state.cardAgreements,previousAgreementId);if(old.brand()!=brand||!old.status().equals("ACTIVE"))throw error("STATE");
            state.cardObligations.stream().filter(o->o.sourceType().equals("AGREEMENT")&&o.sourceId().equals(old.id())&&owed(state,o).signum()>0).map((CardObligation value) -> java.util.Objects.requireNonNull(value).id()).forEach(covered::add);}
        var debt=proposalDebt(state,covered,previousAgreementId);positive(debt);
        var interest=interest(debt,count,customer.plan(),true);var parts=split(debt,interest,count,new BigDecimal("50.00"));
        var proposal=new CardProposal(UUID.randomUUID(),customerId,brand,covered.stream().map((UUID value) -> java.util.Objects.requireNonNull(value).toString()).collect(java.util.stream.Collectors.joining(",")),previousAgreementId,
            debt,agreementRate(customer.plan()),interest,parts.getFirst().total(),debt.add(interest),count,customer.plan(),today(),"PROPOSED",clock.instant());put(state.cardProposals,proposal);
        for(var invoice:state.cardInvoices)if(invoice.brand()==brand&&state.cardObligations.stream().anyMatch(o->covered.contains(o.id())&&o.reference().equals(invoice.reference())))
            put(state.cardProposalInvoices,new CardProposalInvoice(UUID.randomUUID(),customerId,brand,proposal.id(),invoice.id()));
        return finish(state,key,hash,result("PROPOSE","PROPOSED",proposal.id(),proposal.total(),ZERO,ZERO,"A quantidade inclui a entrada; o primeiro período curto conta como mês inteiro"));
    }
    public CardFinanceResult payProposal(UUID customerId,UUID key,UUID proposalId,UUID accountId,String password){
        var customer=lock(customerId);password(customerId,password);var state=load(customer);var chosen=chosenAccount(state,accountId);
        var hash=hash("AGREEMENT_ENTRY",customerId,proposalId,chosen);var replay=replay(state,key,hash);if(replay!=null)return replay;
        var proposal=find(state.cardProposals,proposalId);if(!proposal.status().equals("PROPOSED"))throw error("STATE");
        if(requiredBilling(state,proposal.brand()).creditBalance().signum()>0)throw error("CREDIT_RECONCILIATION");
        var id=UUID.randomUUID();debit(customerId,chosen,proposal.entry(),id,"Entrada de acordo de cartão");
        var credit=activateProposal(state,customer,proposal,proposal.entry(),id);
        put(state.cardPayments,payment(id,customerId,proposal.brand(),"AGREEMENT_ENTRY",proposalId,"ACCOUNT",proposal.entry(),credit,ZERO,"CONFIRMED",chosen,null,null,null,clock.instant()));
        var status=find(state.cardProposals,proposalId).status();
        return finish(state,key,hash,result("AGREEMENT_ENTRY",status,id,proposal.entry(),ZERO,credit,status.equals("ACTIVE")?"Acordo ativado após entrada recebida":"Entrada aplicada como pagamento avulso; nenhum acordo foi ativado"));
    }
    private BigDecimal proposalDebt(CardFinanceState state,Collection<UUID> covered,UUID previousAgreementId){
        BigDecimal total=ZERO;
        for(var id:covered){var row=find(state.cardObligations,id);
            boolean futureOldAgreement=previousAgreementId!=null&&row.sourceType().equals("AGREEMENT")&&row.sourceId().equals(previousAgreementId)&&row.closingAt().isAfter(clock.instant());
            total=total.add(row.remainingPrincipal()).add(futureOldAgreement?ZERO:row.remainingInterest());
            var episode=episode(state,id);if(episode!=null)total=total.add(episode.remainingFine()).add(episode.remainingMora());
        }return total;
    }
    private BigDecimal activateProposal(CardFinanceState state,Customer customer,CardProposal proposal,BigDecimal received,UUID paymentId){
        var covered=proposal.coveredIds().isBlank()?List.<UUID>of():Arrays.stream(proposal.coveredIds().split(",")).map(UUID::fromString).toList();
        var debt=proposalDebt(state,covered,proposal.previousAgreementId());
        if(!proposal.validOn().equals(today())||!proposal.status().equals("PROPOSED")||debt.compareTo(proposal.debt())!=0){
            if(proposal.status().equals("PROPOSED"))put(state.cardProposals,proposal.withStatus("EXPIRED"));
            return allocate(state,proposal.brand(),received,paymentId,"LATE_ENTRY",true);
        }
        var agreement=new CardAgreement(UUID.randomUUID(),customer.id(),proposal.brand(),proposal.id(),proposal.previousAgreementId(),proposal.debt(),proposal.interest(),
            proposal.rate(),proposal.installments(),proposal.plan(),"ACTIVE",clock.instant());put(state.cardAgreements,agreement);
        for(var id:covered){var row=find(state.cardObligations,id);
            put(state.cardObligations,row.withRemainingPrincipal(ZERO).withRemainingInterest(ZERO).withStatus("REPLACED").withFinancedAgreementId(agreement.id()));
            var episode=episode(state,id);if(episode!=null)put(state.cardLateEpisodes,episode.withRemainingFine(ZERO).withRemainingMora(ZERO).withClosed(true));
        }
        if(proposal.previousAgreementId()!=null){var previous=find(state.cardAgreements,proposal.previousAgreementId());put(state.cardAgreements,previous.withStatus("REPLACED"));}
        var parts=split(proposal.debt(),proposal.interest(),proposal.installments(),new BigDecimal("50.00"));
        var config=requiredBilling(state,proposal.brand());var next=YearMonth.from(today());if(!calendar.nominal(config,next).isAfter(today()))next=next.plusMonths(1);
        for(int index=0;index<parts.size();index++){
            var part=parts.get(index);var row=obligation(customer.id(),proposal.brand(),"AGREEMENT",agreement.id(),index+1,index==0?YearMonth.from(today()):next.plusMonths(index-1),config,part.principal(),part.interest());
            if(index==0){row=row.withNominalDue(today()).withEffectiveDue(today()).withClosingAt(clock.instant());put(state.cardObligations,row);
                settle(state,row,paymentId,"AGREEMENT_ENTRY",part.principal(),part.interest(),ZERO);
            }else put(state.cardObligations,row);
        }
        put(state.cardProposals,proposal.withStatus("ACTIVE"));return ZERO;
    }

    public CardFinanceResult reconcile(UUID customerId,UUID key,CardBrand brand,String password){
        var customer=lock(customerId);password(customerId,password);var state=load(customer);
        var hash=hash("CREDIT_RECONCILIATION",customerId,brand);var replay=replay(state,key,hash);if(replay!=null)return replay;
        var id=UUID.randomUUID();var before=requiredBilling(state,brand).creditBalance();reconcileInternal(state,brand,id);
        var remaining=requiredBilling(state,brand).creditBalance();
        return finish(state,key,hash,result("CREDIT_RECONCILIATION","CONFIRMED",id,before.subtract(remaining),ZERO,remaining,"CONFIRMED"));
    }
    private void reconcileInternal(CardFinanceState state,CardBrand brand,UUID id){
        var config=requiredBilling(state,brand);if(config.creditBalance().signum()==0)return;
        var left=allocateClosedCredit(state,brand,config.creditBalance(),id);put(state.billingLines,config.withCreditBalance(left));
    }
    private BigDecimal allocateClosedCredit(CardFinanceState state,CardBrand brand,BigDecimal amount,UUID id){
        for(var row:ordered(state,brand))if(!row.closingAt().isAfter(clock.instant())){
            amount=settleFromAmount(state,row,amount,id,"CREDIT_RECONCILIATION");if(amount.signum()==0)break;
        }closeAgreements(state);return amount;
    }
    private BigDecimal allocate(CardFinanceState state,CardBrand brand,BigDecimal amount,UUID paymentId,String kind,boolean keepExcess){
        for(var row:ordered(state,brand))if(started(state,row)){
            amount=settleFromAmount(state,row,amount,paymentId,kind);if(amount.signum()==0)break;
        }
        if(amount.signum()>0&&keepExcess){var config=requiredBilling(state,brand);put(state.billingLines,config.withCreditBalance(config.creditBalance().add(amount)));}
        closeAgreements(state);return amount;
    }
    private BigDecimal settleFromAmount(CardFinanceState state,CardObligation row,BigDecimal amount,UUID paymentId,String kind){
        var episode=episode(state,row.id());
        if(episode!=null){var mora=amount.min(episode.remainingMora());amount=amount.subtract(mora);var fine=amount.min(episode.remainingFine());amount=amount.subtract(fine);
            put(state.cardLateEpisodes,episode.withRemainingMora(episode.remainingMora().subtract(mora)).withRemainingFine(episode.remainingFine().subtract(fine)));
            if(mora.add(fine).signum()>0){var id=UUID.randomUUID();put(state.cardSettlements,new CardSettlement(id,row.customerId(),row.brand(),paymentId,row.id(),kind,ZERO,ZERO,fine,mora,ZERO,clock.instant()));
                put(state.cardLateAllocations,new CardLateAllocation(id,row.customerId(),row.brand(),paymentId,row.id(),fine,mora,clock.instant()));}}
        var interest=amount.min(row.remainingInterest());amount=amount.subtract(interest);var principal=amount.min(row.remainingPrincipal());amount=amount.subtract(principal);
        if(interest.add(principal).signum()>0)settle(state,row,paymentId,kind,principal,interest,ZERO);
        var current=episode(state,row.id());if(current!=null&&remaining(find(state.cardObligations,row.id())).signum()==0&&current.remainingFine().add(current.remainingMora()).signum()==0)put(state.cardLateEpisodes,current.withClosed(true));
        return amount;
    }
    private void settle(CardFinanceState state,CardObligation row,UUID paymentId,String kind,BigDecimal principal,BigDecimal interest,BigDecimal discount){
        var updated=row.withRemainingPrincipal(row.remainingPrincipal().subtract(principal)).withRemainingInterest(row.remainingInterest().subtract(interest).subtract(discount));
        if(remaining(updated).signum()==0)updated=updated.withStatus(kind.equals("ANTICIPATED")?"ANTICIPATED":kind.equals("REFUND")?"REFUNDED":"PAID");put(state.cardObligations,updated);
        put(state.cardSettlements,new CardSettlement(UUID.randomUUID(),row.customerId(),row.brand(),paymentId,row.id(),kind,interest,principal,ZERO,ZERO,discount,clock.instant()));
    }

    private CardFinanceState load(Customer customer){var state=persistence.load(customer.id());
        for(var instrument:instruments.cards(customer.id()))if(billing(state,instrument.brand())==null)put(state.billingLines,newLine(customer.id(),instrument.brand(),10));
        synchronize(state,customer);return state;
    }
    private void synchronize(CardFinanceState state,Customer customer){
        var justClosed=new HashSet<CardBrand>();
        for(var config:state.billingLines){
            var last=calendar.reference(config,clock.instant());
            for(var reference=YearMonth.parse(config.firstReference());!reference.isAfter(last);reference=reference.plusMonths(1)){
                final String value=reference.toString();var invoice=state.cardInvoices.stream().filter(i->i.brand()==config.brand()&&i.reference().equals(value)).findFirst().orElse(null);
                if(invoice==null){var due=calendar.nominal(config,reference);invoice=new CardInvoice(UUID.randomUUID(),customer.id(),config.brand(),value,due,calendar.effective(due),calendar.closing(config,reference),false,ZERO);put(state.cardInvoices,invoice);}
                BigDecimal billed=ZERO;
                for(var row:List.copyOf(state.cardObligations))if(row.brand()==config.brand()&&row.reference().equals(value)&&!row.status().equals("REPLACED")){
                    if(row.status().equals("PENDING"))put(state.cardObligations,row.withStatus("INVOICED"));
                    if(state.cardInvoiceItems.stream().noneMatch(item->item.obligationId().equals(row.id()))&&remaining(row).signum()>0)
                        put(state.cardInvoiceItems,new CardInvoiceItem(row.id(),customer.id(),row.brand(),invoice.id(),row.id(),row.sourceType(),row.remainingPrincipal(),row.remainingInterest()));
                }
                for(var item:state.cardInvoiceItems)if(item.invoiceId().equals(invoice.id()))billed=billed.add(item.originalPrincipal()).add(item.originalInterest());
                if(!invoice.closed()){
                    boolean closed=!clock.instant().isBefore(invoice.closingAt());if(closed)justClosed.add(config.brand());
                    put(state.cardInvoices,invoice.withClosed(closed).withBilledTotal(billed));
                }
            }
        }
        accrueLate(state,customer);
        for(var config:List.copyOf(state.billingLines))if(justClosed.contains(config.brand())&&config.creditBalance().signum()>0)reconcileInternal(state,config.brand(),UUID.randomUUID());
        closeAgreements(state);
    }
    private void accrueLate(CardFinanceState state,Customer customer){
        for(var row:List.copyOf(state.cardObligations)){
            if(!row.effectiveDue().isBefore(today())||!started(state,row)||remaining(row).signum()==0)continue;
            var episode=episode(state,row.id());
            if(episode==null){var fineRate=new BigDecimal(customer.plan()==br.com.spbank.shared.application.model.CustomerPlan.STANDARD?"0.02":"0.005");
                var moraRate=new BigDecimal(customer.plan()==br.com.spbank.shared.application.model.CustomerPlan.STANDARD?"0.01":"0.0025");var fine=round(remaining(row).multiply(fineRate));
                episode=new CardLateEpisode(row.id(),row.customerId(),row.brand(),row.id(),customer.plan(),fineRate,moraRate,remaining(row),row.effectiveDue(),BigDecimal.ZERO.setScale(8),fine,ZERO,fine,ZERO,false);}
            long days=ChronoUnit.DAYS.between(episode.lastProcessed(),today());
            if(days>0){var accrued=episode.accruedRaw().add(remaining(row).multiply(episode.moraRate()).multiply(BigDecimal.valueOf(days)).divide(new BigDecimal("30"),8,RoundingMode.HALF_UP));
                var charged=round(accrued);var increase=charged.subtract(episode.moraCharged());episode=episode.withAccruedRaw(accrued).withMoraCharged(charged).withRemainingMora(episode.remainingMora().add(increase)).withLastProcessed(today());}
            put(state.cardLateEpisodes,episode);
        }
    }
    private void closeAgreements(CardFinanceState state){for(var agreement:List.copyOf(state.cardAgreements))if(agreement.status().equals("ACTIVE")
        &&state.cardObligations.stream().filter(o->o.sourceType().equals("AGREEMENT")&&o.sourceId().equals(agreement.id())).allMatch(o->owed(state,o).signum()==0))put(state.cardAgreements,agreement.withStatus("CLOSED"));}
    private void save(CardFinanceState state){
        for(var config:state.billingLines){var current=line(config.customerId(),config.brand());
            var committed=state.cardObligations.stream().filter(o->o.brand()==config.brand()).map(this::remaining).reduce(ZERO,(left, right) -> java.util.Objects.requireNonNull(left).add(java.util.Objects.requireNonNull(right)))
                .add(state.cardLateEpisodes.stream().filter(e->e.brand()==config.brand()).map(e->e.remainingFine().add(e.remainingMora())).reduce(ZERO,(left, right) -> java.util.Objects.requireNonNull(left).add(java.util.Objects.requireNonNull(right))));
            var existing=new ArrayList<>(lines.lines(config.customerId()));existing.removeIf(l->l.brand()==config.brand());existing.add(new CardCreditLine(config.brand(),current.allocated(),committed));lines.saveLines(config.customerId(),existing);
        }persistence.save(state);
    }
    private BillingLine newLine(UUID customerId,CardBrand brand,int day){var temporary=new BillingLine(UUID.randomUUID(),customerId,brand,day,0,"","",ZERO);
        return temporary.withFirstReference(calendar.reference(temporary,clock.instant()).toString());}
    private CardObligation obligation(UUID customerId,CardBrand brand,String type,UUID sourceId,int sequence,YearMonth reference,BillingLine config,BigDecimal principal,BigDecimal interest){
        var due=calendar.nominal(config,reference);return new CardObligation(UUID.randomUUID(),customerId,brand,type,sourceId,sequence,reference.toString(),due,calendar.effective(due),calendar.closing(config,reference),principal,interest,principal,interest,"PENDING",null);}
    private CardPayment payment(UUID id,UUID customerId,CardBrand brand,String purpose,UUID proposalId,String method,BigDecimal amount,BigDecimal credit,BigDecimal discount,String status,
            UUID accountId,UUID adminId,String reason,String code,Instant paidAt){return new CardPayment(id,customerId,brand,purpose,proposalId,method,amount,status.equals("PENDING")?ZERO:amount.subtract(credit),credit,discount,status,accountId,adminId,reason,code,clock.instant(),paidAt);}
    private BigDecimal remaining(CardObligation row){return row.remainingPrincipal().add(row.remainingInterest());}
    private BigDecimal owed(CardFinanceState state,CardObligation row){var episode=episode(state,row.id());return remaining(row).add(episode==null?ZERO:episode.remainingFine().add(episode.remainingMora()));}
    private boolean started(CardFinanceState state,CardObligation row){return state.cardInvoices.stream().anyMatch(i->i.brand()==row.brand()&&i.reference().equals(row.reference()));}
    private List<CardObligation> ordered(CardFinanceState state,CardBrand brand){return state.cardObligations.stream().filter(o->o.brand()==brand)
        .sorted(Comparator.comparing((CardObligation value) -> java.util.Objects.requireNonNull(value).effectiveDue()).thenComparingInt((CardObligation value) -> java.util.Objects.requireNonNull(value).sequence()).thenComparing((CardObligation value) -> java.util.Objects.requireNonNull(value).id())).toList();}
    private boolean overdue(CardFinanceState state,CardBrand brand){return state.cardObligations.stream().anyMatch(o->o.brand()==brand&&remaining(o).signum()>0&&o.effectiveDue().isBefore(today()))
        ||state.cardLateEpisodes.stream().anyMatch(e->e.brand()==brand&&e.remainingFine().add(e.remainingMora()).signum()>0);}
    private BigDecimal payable(CardFinanceState state,CardBrand brand){return state.cardObligations.stream().filter(o->o.brand()==brand&&started(state,o)).map(this::remaining).reduce(ZERO,(left, right) -> java.util.Objects.requireNonNull(left).add(java.util.Objects.requireNonNull(right)))
        .add(state.cardLateEpisodes.stream().filter(e->e.brand()==brand).map(e->e.remainingFine().add(e.remainingMora())).reduce(ZERO,(left, right) -> java.util.Objects.requireNonNull(left).add(java.util.Objects.requireNonNull(right))));}
    private CardLateEpisode episode(CardFinanceState state,UUID obligationId){return state.cardLateEpisodes.stream().filter(e->e.obligationId().equals(obligationId)).findFirst().orElse(null);}
    private BillingLine billing(CardFinanceState state,CardBrand brand){return state.billingLines.stream().filter(l->l.brand()==brand).findFirst().orElse(null);}
    private BillingLine requiredBilling(CardFinanceState state,CardBrand brand){var value=billing(state,brand);if(value==null)throw error("BRAND");return value;}
    private CardCreditLine line(UUID customerId,CardBrand brand){return lines.lines(customerId).stream().filter(l->l.brand()==brand).findFirst().orElse(new CardCreditLine(brand,ZERO,ZERO));}
    private UUID chosenAccount(CardFinanceState state,UUID explicit){if(explicit!=null)return explicit;return state.cardPaymentPreferences.stream().findFirst().map(value->Objects.requireNonNull(value).accountId()).orElseThrow(()->error("ACCOUNT"));}
    private Account ownedAccount(UUID customerId,UUID accountId,boolean forUpdate){var account=forUpdate?accounts.findAllForUpdate(List.of(accountId)).get(accountId):accounts.findById(accountId).orElse(null);
        if(account==null||!account.getCustomerId().equals(customerId)||!account.isActive())throw error("ACCOUNT");return account;}
    private void debit(UUID customerId,UUID accountId,BigDecimal amount,UUID reference,String description){var account=ownedAccount(customerId,accountId,true);account.debit(positive(amount));
        accounts.saveAll(List.of(account));entries.saveAll(List.of(AccountEntry.create(account,reference,EntryReferenceType.CARD,EntryType.CARD_PAYMENT,EntryDirection.DEBIT,amount,description,clock.instant())));}
    private Customer lock(UUID id){return customers.findCustomerByIdForUpdate(id).filter((Customer value) -> java.util.Objects.requireNonNull(value).active()).orElseThrow(UnauthorizedException::new);}
    private void password(UUID customerId,String password){if(customers.findCredentialByCustomerId(customerId).filter(c->c.active()&&PasswordHasher.matches(password,c.passwordHash())).isEmpty())throw error("PASSWORD");}
    public void administrator(UUID id){administrators.findCredentialById(id).filter(a->a.active()).orElseThrow(UnauthorizedException::new);}
    public CardFinanceState administrativeOverview(UUID administratorId,UUID customerId){administrator(administratorId);return overview(customerId);}
    private static <T>T find(List<T> rows,UUID id){return rows.stream().filter(row -> row instanceof CardFinanceRecord record && record.id().equals(id)).findFirst().orElseThrow(()->new NotFoundException("CARD_FINANCE_NOT_FOUND"));}
    private static String text(String value){if(value==null||value.isBlank()||value.strip().length()>240)throw error("TEXT");return value.strip();}
    private static void validDay(int day){if(!List.of(5,10,15,20,25).contains(day))throw error("DUE_DAY");}
    private LocalDate today(){return calendar.today(clock.instant());}
    private String hash(String action,UUID actor,Object... values){var parts=new ArrayList<String>();parts.add(action);parts.add(actor.toString());for(var value:values)parts.add(String.valueOf(value));return PasswordHasher.tokenHash(json.writeValueAsString(parts));}
    private @org.jspecify.annotations.Nullable CardFinanceResult replay(CardFinanceState state,UUID key,String hash){
        Objects.requireNonNull(key,"Idempotency-Key");
        var found=state.cardFinancialOperations.stream().filter(op->op.key().equals(key)).findFirst();
        if(found.isEmpty()) return null;
        var operation=found.orElseThrow();
        if(!operation.fingerprint().equals(hash)) throw new IdempotencyConflictException();
        return json.readValue(operation.responseJson(),CardFinanceResult.class);
    }
    private CardFinanceResult finish(CardFinanceState state,UUID key,String hash,CardFinanceResult result){put(state.cardFinancialOperations,new CardFinancialOperation(result.operationId(),stateCustomer(state),key,hash,json.writeValueAsString(result)));save(state);return result;}
    private UUID stateCustomer(CardFinanceState state){if(!state.billingLines.isEmpty())return state.billingLines.getFirst().customerId();if(!state.cardAttempts.isEmpty())return state.cardAttempts.getFirst().customerId();throw error("BRAND");}
    private CardFinanceResult result(String action,String status,UUID objectId,BigDecimal amount,BigDecimal discount,BigDecimal credit,String message){return new CardFinanceResult(UUID.randomUUID(),action,status,objectId,amount,discount,credit,message,clock.instant());}
}
