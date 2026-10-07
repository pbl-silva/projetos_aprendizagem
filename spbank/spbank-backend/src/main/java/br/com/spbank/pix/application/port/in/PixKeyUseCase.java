package br.com.spbank.pix.application.port.in;

import br.com.spbank.pix.application.model.PixKey;
import br.com.spbank.pix.application.model.PixKeyType;

import java.util.List;
import java.util.UUID;

public interface PixKeyUseCase {

    PixKey registerKey(
        UUID accountId,
        PixKeyType type,
        String value
    );

    List<PixKey> listKeys(
        UUID accountId
    );

    void deactivateKey(
        UUID accountId,
        UUID keyId
    );
}