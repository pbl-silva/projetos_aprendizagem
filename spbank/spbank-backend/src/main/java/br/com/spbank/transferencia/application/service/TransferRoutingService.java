package br.com.spbank.transferencia.application.service;

import br.com.spbank.conta.application.exception.AccountInactiveException;
import br.com.spbank.conta.application.model.Account;
import br.com.spbank.conta.application.model.BankInstitution;
import br.com.spbank.conta.application.port.in.AccountLookup;
import br.com.spbank.conta.application.port.out.AccountPersistence;
import br.com.spbank.conta.application.port.out.BankInstitutionCatalog;
import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.NotFoundException;
import br.com.spbank.transferencia.application.model.TransferType;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public final class TransferRoutingService {

    private final AccountPersistence accounts;
    private final BankInstitutionCatalog institutions;

    public TransferRoutingService(
            AccountPersistence accounts,
            BankInstitutionCatalog institutions
    ) {
        this.accounts = accounts;
        this.institutions = institutions;
    }

    public TransferRoute resolve(
            AccountLookup target,
            String recipientName,
            String recipientDocument
    ) {

        BankInstitution institution = institutions
                .findActiveByCode(target.bankCode())
                .orElseThrow(() -> new BusinessException(
                        "BANK_UNAVAILABLE",
                        "bank.unavailable"
                ));

        TransferType type = institution.internal()
                ? TransferType.INTERNAL
                : TransferType.TED;

        Optional<Account> targetAccount = Optional.empty();

        if (type == TransferType.INTERNAL) {

            Account account = requireActive(
                    accounts.findTarget(target)
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            "TARGET_ACCOUNT_NOT_FOUND"
                                    )
                            )
            );

            validateRecipient(
                    account,
                    recipientName,
                    recipientDocument
            );

            targetAccount = Optional.of(account);
        }

        return new TransferRoute(
                institution,
                type,
                targetAccount
        );
    }

    public void validateRecipient(
            Account target,
            String recipientName,
            String recipientDocument
    ) {

        if (!digits(target.getHolderDocument())
                .equals(digits(recipientDocument))
                || !text(target.getHolderName())
                .equals(text(recipientName))) {

            throw new BusinessException(
                    "RECIPIENT_MISMATCH",
                    "transfer.recipient-mismatch"
            );
        }
    }

    private static Account requireActive(Account account) {

        if (!account.isActive()) {
            throw new AccountInactiveException();
        }

        return account;
    }

    public static String digits(String value) {

        return value.replaceAll("\\D", "");
    }

    private static String text(String value) {

        String ascii = Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "");

        return ascii
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    public record TransferRoute(
            BankInstitution institution,
            TransferType type,
            Optional<Account> targetAccount
    ) {
    }
}