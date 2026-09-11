package br.com.spbank.pix.application.port.out;

import br.com.spbank.pix.application.model.PixPayment;

public interface PixSettlementPort {

    String settle(PixPayment payment);
}