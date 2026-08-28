package br.com.spbank.conta.application.port.in;

import br.com.spbank.conta.application.model.BankInstitution;

import java.util.List;

public interface ListBankInstitutionsUseCase {

    List<BankInstitution> listActive();
}