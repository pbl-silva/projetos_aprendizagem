package br.com.spbank.cartao.application.port.out;

import br.com.spbank.cartao.application.model.*;

public interface CardCredentialVault {
    CardCredentials issue(CardBrand brand);
    CardDetails reveal(CardInstrument card);
}
