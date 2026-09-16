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
                + "paymentId={} status={} "
                + "sourceAccountRef={} amount={} "
                + "occurredAt={} failureCode={} "
                + "maskedKey={} maskedDocument={} "
                + "keyHashRef={}",
            event.paymentId(),
            event.status(),
            maskedReference(
                event.sourceAccountId()
            ),
            event.amount(),
            event.occurredAt(),
            event.failureCode(),
            event.maskedKey(),
            event.maskedDocument(),
            maskedHash(
                event.keyHash()
            )
        );
    }

    static String maskedReference(
        UUID accountId
    ) {
        if (accountId == null) {
            return "unknown";
        }

        String value =
            accountId.toString();

        return "***"
            + value.substring(
                value.length() - 8
            );
    }

    static String maskedHash(
        String hash
    ) {
        if (hash == null || hash.length() < 8) {
            return "unknown";
        }

        return "***"
            + hash.substring(
                hash.length() - 8
            );
    }
}