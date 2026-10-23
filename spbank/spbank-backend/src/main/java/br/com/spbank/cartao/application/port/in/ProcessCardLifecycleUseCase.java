package br.com.spbank.cartao.application.port.in;

import java.util.UUID;

public interface ProcessCardLifecycleUseCase {
    void process(UUID cardId);
}
