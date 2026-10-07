package br.com.spbank.pix.application.port.out;

public interface PixBusinessLogPort {

    void publish(
        PixBusinessEvent event
    );
}