package br.com.spbank.pagamento.adapter.in.scheduler;
import br.com.spbank.pagamento.application.port.in.PaymentUseCase;
import br.com.spbank.pagamento.application.port.out.PaymentPersistence;
import java.time.*;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.*;
@Component
public class ScheduledPaymentProcessor {
 private static final Logger LOG=LoggerFactory.getLogger(ScheduledPaymentProcessor.class);
 private final PaymentPersistence store;private final PaymentUseCase service;private final Clock clock;
 public ScheduledPaymentProcessor(PaymentPersistence store,PaymentUseCase service,Clock clock){this.store=store;this.service=service;this.clock=clock;}
 @Scheduled(fixedDelayString="${spbank.payment.processing-delay-ms:60000}",initialDelayString="${spbank.payment.processing-initial-delay-ms:60000}") public void process(){for(var id:store.due(LocalDate.ofInstant(clock.instant(),ZoneId.of("America/Sao_Paulo")),100))try{service.execute(id);}catch(RuntimeException failure){LOG.error("Payment execution failed id={} type={}",id,failure.getClass().getSimpleName());}}
}
