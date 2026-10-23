package br.com.spbank.investimento.adapter.in.scheduler;
import br.com.spbank.investimento.application.port.in.InvestmentUseCase;
import br.com.spbank.investimento.application.port.out.InvestmentPersistence;
import br.com.spbank.investimento.application.service.InvestmentBusinessCalendar;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public final class InvestmentApplicationProcessor {
    private static final Logger LOG=LoggerFactory.getLogger(InvestmentApplicationProcessor.class);
    private final InvestmentPersistence persistence;private final InvestmentUseCase investments;
    private final InvestmentBusinessCalendar calendar;private final Clock clock;
    public InvestmentApplicationProcessor(InvestmentPersistence persistence,InvestmentUseCase investments,InvestmentBusinessCalendar calendar,Clock clock){this.persistence=persistence;this.investments=investments;this.calendar=calendar;this.clock=clock;}
    @Scheduled(fixedDelayString="${spbank.investment.processing-delay-ms:60000}",initialDelayString="${spbank.investment.processing-initial-delay-ms:60000}")
    public void processDue(){var today=calendar.today(clock);if(!calendar.businessDay(today))return;
        for(var id:persistence.due(today,100))try{investments.process(id);}catch(RuntimeException failure){LOG.error("Falha na aplicação de investimento applicationId={} exceptionType={}",id,failure.getClass().getSimpleName());}}
}
