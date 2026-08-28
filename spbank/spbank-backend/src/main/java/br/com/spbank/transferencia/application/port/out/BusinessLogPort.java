package br.com.spbank.transferencia.application.port.out;

public interface BusinessLogPort {

    void publish(TransferBusinessEvent event);
}