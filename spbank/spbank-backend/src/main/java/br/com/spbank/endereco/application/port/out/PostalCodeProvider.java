package br.com.spbank.endereco.application.port.out;

import br.com.spbank.endereco.application.model.PostalAddress;
import java.util.Optional;

public interface PostalCodeProvider {
    Optional<PostalAddress> find(String postalCode);
}
