package br.com.spbank.transferencia.adapter.in.api.rest.dto;

import br.com.spbank.conta.application.model.AccountType;
import br.com.spbank.shared.validation.CpfOrCnpj;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferCreationDto(

        @NotBlank(message = "{recipient.name.required}")
        @Size(max = 120, message = "{recipient.name.size}")
        String recipientName,

        @NotBlank(message = "{recipient.document.required}")
        @CpfOrCnpj(message = "{recipient.document.invalid}")
        String recipientDocument,

        @NotBlank(message = "{bank.code.required}")
        @Size(max = 8, message = "{bank.code.size}")
        String bankCode,

        @NotBlank(message = "{branch.required}")
        @Size(max = 10, message = "{branch.size}")
        String branch,

        @NotBlank(message = "{account.number.required}")
        @Size(max = 20, message = "{account.number.size}")
        String accountNumber,

        @NotNull(message = "{account.type.required}")
        AccountType accountType,

        @NotNull(message = "{transfer.amount.required}")
        @DecimalMin(
                value = "0.01",
                message = "{transfer.amount.minimum}"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "{transfer.amount.format}"
        )
        BigDecimal amount,

        @FutureOrPresent(message = "{transfer.date.invalid}")
        LocalDate scheduledFor

) {
}