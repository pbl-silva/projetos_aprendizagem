package br.com.spbank.transferencia.application.port.in;

import br.com.spbank.transferencia.application.model.Transfer;

import java.util.UUID;

public interface CancelTransferUseCase {

    Transfer cancel(
            UUID transferId,
            UUID sourceAccountId
    );
}