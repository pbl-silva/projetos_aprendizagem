
package br.com.spbank.conta.adapter.in.api.rest.controller;

import br.com.spbank.conta.adapter.in.api.rest.dto.BankInstitutionDto;
import br.com.spbank.conta.application.port.in.ListBankInstitutionsUseCase;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/banks")
public final class BankController {

    private final ListBankInstitutionsUseCase institutions;

    public BankController(
            ListBankInstitutionsUseCase institutions
    ) {
        this.institutions = institutions;
    }

    @GetMapping
    public List<BankInstitutionDto> listActive() {
        return institutions
                .listActive()
                .stream()
                .map(bank -> new BankInstitutionDto(
                        bank.studyCode(),
                        bank.name()
                ))
                .toList();
    }
}
