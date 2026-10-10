
package br.com.spbank.transferencia.adapter.out.log;

import br.com.spbank.transferencia.application.port.out.TransferBusinessEvent;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public final class TransferBusinessEventListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    TransferBusinessEventListener.class
            );

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void on(TransferBusinessEvent event) {

        LOG.info(
                "businessEvent=TRANSFER_STATUS_CHANGED transferId={} status={} "
                        + "sourceAccountRef={} amount={} fee={} occurredAt={} failureCode={}",
                event.transferId(),
                event.status(),
                maskedReference(event.sourceAccountId()),
                event.amount(),
                event.fee(),
                event.occurredAt(),
                event.failureCode()
        );
    }

    static String maskedReference(UUID accountId) {

        if (accountId == null) {
            return "unknown";
        }

        String value = accountId.toString();

        return "***" + value.substring(
                value.length() - 8
        );
    }
}
