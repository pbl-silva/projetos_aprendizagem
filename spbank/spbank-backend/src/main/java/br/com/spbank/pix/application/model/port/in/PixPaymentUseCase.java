package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.Pix;

import java.util.UUID;

public interface PixPaymentUseCase {

    Pix preview(UUID accountId, PixPreviewCommand command);

    Pix create(UUID accountId, PixCreationCommand command);

    Pix findById(UUID accountId, UUID pixId);

    void cancel(UUID accountId, UUID pixId);
}