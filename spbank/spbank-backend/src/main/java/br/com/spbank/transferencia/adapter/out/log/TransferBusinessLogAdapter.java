
package br.com.spbank.transferencia.adapter.out.log;

import br.com.spbank.transferencia.application.port.out.BusinessLogPort;
import br.com.spbank.transferencia.application.port.out.TransferBusinessEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public final class TransferBusinessLogAdapter
        implements BusinessLogPort {

    private final ApplicationEventPublisher events;

    public TransferBusinessLogAdapter(
            ApplicationEventPublisher events
    ) {
        this.events = events;
    }

    @Override
    public void publish(
            TransferBusinessEvent event
    ) {
        events.publishEvent(java.util.Objects.requireNonNull(event));
    }
}
