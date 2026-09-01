package br.com.spbank.transferencia.application.port.in;

import br.com.spbank.transferencia.application.model;

import java.util.UUID;

public interface GetTransferUseCase {

    Transfer get(
            UUID transferId,
            UUID sourceAccountId
    );
}