package br.com.spbank.transferencia.application.port.in;

import br.com.spbank.transferencia.application.model.transfer.Transfer;

import java.util.List;
import java.util.UUID;

public interface ListScheduledTransfersUseCase {

    List<Transfer> list(UUID sourceAccountId);
}