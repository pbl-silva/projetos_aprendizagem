package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixPayment;

import java.util.UUID;

public interface ExecuteScheduledPixUseCase {

    PixPayment execute(UUID paymentId);
}