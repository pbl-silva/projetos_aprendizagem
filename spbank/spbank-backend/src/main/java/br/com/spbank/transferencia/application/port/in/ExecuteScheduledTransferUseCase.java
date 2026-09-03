package br.com.spbank.transferencia.application.port.in;

import br.com.spbank.transferencia.application.model.Transfer;

import java.util.UUID;

public interface ExecuteScheduledTransferUseCase {

    Transfer execute(UUID transferId);
}