package br.com.spbank.pix.application.port.out;

public interface BusinessLogPort {

    void publish(PixBusinessEvent event);
}