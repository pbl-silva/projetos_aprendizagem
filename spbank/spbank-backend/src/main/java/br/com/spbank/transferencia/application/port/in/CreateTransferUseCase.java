package br.com.spbank.transferencia.application.port.in;

import br.com.spbank.transferencia.application.model;

public interface CreateTransferUseCase {

    Transfer create(CreateTransferCommand command);
}