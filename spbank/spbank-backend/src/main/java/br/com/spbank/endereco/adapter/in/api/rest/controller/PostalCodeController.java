package br.com.spbank.endereco.adapter.in.api.rest.controller;

import br.com.spbank.endereco.application.model.PostalCodeLookup;
import br.com.spbank.endereco.application.usecase.PostalCodeLookupService;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/address")
public final class PostalCodeController {
    private final PostalCodeLookupService postalCodes;

    public PostalCodeController(PostalCodeLookupService postalCodes) {
        this.postalCodes = postalCodes;
    }

    @GetMapping("/postal-codes/{postalCode}")
    public PostalCodeLookup find(
            @PathVariable @Pattern(regexp = "\\d{8}", message = "Informe um CEP com oito dígitos") String postalCode
    ) {
        return postalCodes.find(postalCode);
    }
}
