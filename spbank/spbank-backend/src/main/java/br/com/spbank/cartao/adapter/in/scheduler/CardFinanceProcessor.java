package br.com.spbank.cartao.adapter.in.scheduler;
import br.com.spbank.cartao.application.usecase.CardFinanceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;

@Component
public final class CardFinanceProcessor {
    private final CardFinanceService finance;
    public CardFinanceProcessor(CardFinanceService finance){this.finance=finance;}
    @Scheduled(fixedDelayString="${spbank.card.finance-scheduler-delay:60000}")
    public void process(){for(var customer:finance.customerIds())try{finance.processCustomer(customer);}
        catch(RuntimeException ex){LoggerFactory.getLogger(CardFinanceProcessor.class).error("Falha no processamento financeiro de cartão customerId={} exceptionType={}",customer,ex.getClass().getSimpleName());}}
}
