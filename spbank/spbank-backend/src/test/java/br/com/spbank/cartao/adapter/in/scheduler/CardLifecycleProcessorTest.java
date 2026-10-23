package br.com.spbank.cartao.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.spbank.cartao.application.port.in.ProcessCardLifecycleUseCase;
import br.com.spbank.cartao.application.port.out.CardInstrumentPersistence;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class CardLifecycleProcessorTest {
    @Test void usesLocalDateLimitsTheBatchAndContinuesAfterOneFailureWithoutLoggingSecrets() {
        var cards=java.util.Objects.requireNonNull(mock(CardInstrumentPersistence.class));
        var lifecycle=java.util.Objects.requireNonNull(mock(ProcessCardLifecycleUseCase.class));
        var first=UUID.randomUUID();var second=UUID.randomUUID();
        var today=LocalDate.of(2026,9,30);
        when(cards.findLifecycleDueIds(today,today.plusDays(30),100)).thenReturn(List.of(first,second));
        java.util.Objects.requireNonNull(doThrow(new IllegalStateException("sensitive-card-credential")).when(lifecycle)).process(first);
        var logger=(Logger)LoggerFactory.getLogger(CardLifecycleProcessor.class);
        var output=new ListAppender<ILoggingEvent>();output.start();logger.addAppender(output);
        try {
            new CardLifecycleProcessor(cards,lifecycle,Clock.fixed(Instant.parse("2026-10-01T02:59:59Z"),ZoneOffset.UTC),"America/Sao_Paulo").processDueBatch();
            java.util.Objects.requireNonNull(verify(cards)).findLifecycleDueIds(today,today.plusDays(30),100);
            java.util.Objects.requireNonNull(verify(lifecycle)).process(first);java.util.Objects.requireNonNull(verify(lifecycle)).process(second);
            assertThat(output.list).hasSize(1);
            assertThat(output.list.getFirst().getFormattedMessage()).contains(first.toString(),"IllegalStateException");
        assertThat(output.list.getFirst().getFormattedMessage()).doesNotContain("sensitive-card-credential");
            assertThat(output.list.getFirst().getThrowableProxy()).isNull();
        } finally {logger.detachAppender(output);output.stop();}
    }
    @Test void noCandidatesMeansNoTransactions() {
        var cards=java.util.Objects.requireNonNull(mock(CardInstrumentPersistence.class));var lifecycle=java.util.Objects.requireNonNull(mock(ProcessCardLifecycleUseCase.class));
        when(cards.findLifecycleDueIds(any(),any(),anyInt())).thenReturn(List.of());
        new CardLifecycleProcessor(cards,lifecycle,Clock.systemUTC(),"America/Sao_Paulo").processDueBatch();
        verifyNoInteractions(lifecycle);
    }
}
