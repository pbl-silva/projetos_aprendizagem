package br.com.spbank.investimento.adapter.in.scheduler;
import br.com.spbank.investimento.application.port.out.InvestmentRedemptions;
import br.com.spbank.investimento.application.port.in.InvestmentRedemptionUseCase;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.*;
@Component
public class InvestmentRedemptionProcessor {
 private static final Logger LOG=LoggerFactory.getLogger(InvestmentRedemptionProcessor.class);
 private final InvestmentRedemptions rows;private final InvestmentRedemptionUseCase service;
 public InvestmentRedemptionProcessor(InvestmentRedemptions rows,InvestmentRedemptionUseCase service){this.rows=rows;this.service=service;}
 @Scheduled(fixedDelayString="${spbank.investment.redemption.processing-delay-ms:60000}",initialDelayString="${spbank.investment.redemption.processing-initial-delay-ms:60000}") public void process(){for(var customer:rows.customers())try{service.process(customer);}catch(RuntimeException failure){LOG.error("Redemption processing failed customer={} type={}",customer,failure.getClass().getSimpleName());}}
}
