package br.com.spbank.conta.application.model;

import br.com.spbank.conta.application.exception.InsufficientFundsException;
import br.com.spbank.shared.application.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public final class Account {

    private final UUID id;
    private final String holderName;
    private final String holderDocument;
    private final String bankCode;
    private final String branch;
    private final String accountNumber;
    private final AccountType accountType;
    private final AccountPlan accountPlan;
    private BigDecimal balance;
    private final boolean active;

    public Account(
            UUID id,
            String holderName,
            String holderDocument,
            String bankCode,
            String branch,
            String accountNumber,
            AccountType accountType,
            AccountPlan accountPlan,
            BigDecimal balance,
            boolean active) {

        this.id = id;
        this.holderName = holderName;
        this.holderDocument = holderDocument;
        this.bankCode = bankCode;
        this.branch = branch;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.accountPlan = accountPlan;
        this.balance = money(balance);
        this.active = active;
    }

    public void requireAvailable(BigDecimal total) {
        requirePositive(total);

        if (balance.compareTo(total) < 0) {
            throw new InsufficientFundsException();
        }
    }

    public void debit(BigDecimal amount) {
        requireAvailable(amount);
        balance = balance.subtract(money(amount));
    }

    public void credit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(money(amount));
    }

    private static void requirePositive(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.scale() > 2) {
            throw new BusinessException(
                    "INVALID_AMOUNT",
                    "account.invalid-amount"
            );
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    public UUID getId() {
        return id;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getHolderDocument() {
        return holderDocument;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getBranch() {
        return branch;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountPlan getAccountPlan() {
        return accountPlan;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }
}