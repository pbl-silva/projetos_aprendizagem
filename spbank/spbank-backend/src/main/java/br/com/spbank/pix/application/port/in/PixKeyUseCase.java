package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixKey;

import java.util.List;
import java.util.UUID;

public interface PixKeyUseCase {

    List<PixKey> listActive(UUID accountId);

    PixKey register(PixKeyRegistrationCommand command);

    void deactivate(UUID accountId, UUID keyId);
}