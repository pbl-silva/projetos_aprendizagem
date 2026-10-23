package br.com.spbank.pix.adapter.out.log;

import br.com.spbank.pix.application.port.out.PixBusinessEvent;
import br.com.spbank.pix.application.port.out.PixBusinessLogPort;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public final class PixBusinessLogAdapter
        implements PixBusinessLogPort {

    private final ApplicationEventPublisher events;

    public PixBusinessLogAdapter(
        ApplicationEventPublisher events
    ) {
        this.events = events;
    }

    @Override
    public void publish(
        PixBusinessEvent event
    ) {
        events.publishEvent(java.util.Objects.requireNonNull(event));
    }
}