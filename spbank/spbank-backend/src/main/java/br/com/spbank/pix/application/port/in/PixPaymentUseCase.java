package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixPayment;

import java.util.List;
import java.util.UUID;

public interface PixPaymentUseCase {

    PixPreview preview(
        PixCreationCommand command
    );

    PixPayment create(
        PixCreationCommand command
    );

    PixPayment get(
        UUID accountId,
        UUID pixId
    );

    List<PixPayment> listScheduled(
        UUID accountId
    );

    PixPayment cancel(
        UUID accountId,
        UUID pixId
    );

    int cancelRecurrence(
        UUID accountId,
        UUID recurrenceId
    );
}