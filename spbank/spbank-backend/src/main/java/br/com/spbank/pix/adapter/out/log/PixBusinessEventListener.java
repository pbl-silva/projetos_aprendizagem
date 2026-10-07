package br.com.spbank.pix.adapter.out.log;

import br.com.spbank.pix.application.port.out.PixBusinessEvent;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public final class PixBusinessEventListener {

    private static final Logger LOG =
        LoggerFactory.getLogger(
            PixBusinessEventListener.class
        );

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
    )
    public void on(
        PixBusinessEvent event
    ) {
        LOG.info(
            "businessEvent=PIX_STATUS_CHANGED "
                + "pixId={} status={} "
                + "sourceAccountRef={} amount={} "
                + "keyType={} keyFingerprint={} "
                + "occurredAt={} failureCode={}",
            event.pixId(),
            event.status(),
            maskedReference(
                event.sourceAccountId()
            ),
            event.amount(),
            event.keyType(),
            fingerprint(
                event.keyFingerprint()
            ),
            event.occurredAt(),
            event.failureCode()
        );
    }

    static String maskedReference(
        UUID id
    ) {
        if (id == null) {
            return "unknown";
        }

        String value =
            id.toString();

        return "***"
            + value.substring(
                value.length() - 8
            );
    }

    static String fingerprint(
        String hash
    ) {
        return hash == null
            || hash.length() < 12

            ? "unknown"

            : hash.substring(
                0,
                12
            );
    }
}