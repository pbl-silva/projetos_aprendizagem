package br.com.spbank.investimento.adapter.in.scheduler;
import br.com.spbank.investimento.application.port.in.InvestmentUseCase;
import br.com.spbank.investimento.application.port.out.InvestmentReferenceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public final class InvestmentValuationProcessor {
    private static final Logger LOG=LoggerFactory.getLogger(InvestmentValuationProcessor.class);
    private final InvestmentReferenceData reference;private final InvestmentUseCase investments;
    public InvestmentValuationProcessor(InvestmentReferenceData reference,InvestmentUseCase investments){this.reference=reference;this.investments=investments;}
    @Scheduled(fixedDelayString="${spbank.investment.valuation-delay-ms:60000}",initialDelayString="${spbank.investment.valuation-initial-delay-ms:60000}")
    public void process(){for(var customer:reference.customersWithPositions())try{investments.processValuations(customer);}catch(RuntimeException failure){LOG.error("Falha na avaliação de investimentos customerId={} exceptionType={}",customer,failure.getClass().getSimpleName());}}
}
