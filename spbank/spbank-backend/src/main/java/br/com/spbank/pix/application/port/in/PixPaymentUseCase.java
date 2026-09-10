package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixPayment;

import java.util.List;
import java.util.UUID;

public interface PixPaymentUseCase {

    PixPreview preview(PixPreviewCommand command);

    PixPayment create(PixCreationCommand command);

    PixPayment get(UUID paymentId, UUID sourceAccountId);

    List<PixPayment> listScheduled(UUID sourceAccountId);

    PixPayment cancel(UUID paymentId, UUID sourceAccountId);

    void cancelRecurrence(UUID recurrenceId, UUID sourceAccountId);
}