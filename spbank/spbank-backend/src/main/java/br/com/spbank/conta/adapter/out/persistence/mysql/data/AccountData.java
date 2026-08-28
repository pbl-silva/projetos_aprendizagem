package br.com.spbank.conta.adapter.out.persistence.mysql.data;

import br.com.spbank.conta.adapter.out.persistence.mysql.converter.AccountPlanConverter;
import br.com.spbank.conta.adapter.out.persistence.mysql.converter.AccountTypeConverter;
import br.com.spbank.conta.application.model.AccountPlan;
import br.com.spbank.conta.application.model.AccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contas")
public class AccountData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "nome_titular")
    private String holderName;

    @Column(name = "documento_titular")
    private String holderDocument;

    @Column(name = "codigo_banco")
    private String bankCode;

    @Column(name = "agencia")
    private String branch;

    @Column(name = "numero_conta")
    private String accountNumber;

    @Convert(converter = AccountTypeConverter.class)
    @Column(name = "tipo_conta")
    private AccountType accountType;

    @Convert(converter = AccountPlanConverter.class)
    @Column(name = "plano_conta")
    private AccountPlan accountPlan;

    @Column(name = "saldo")
    private BigDecimal balance;

    @Column(name = "ativa")
    private boolean active;

    protected AccountData() {
    }

    public AccountData(
            UUID id,
            String holderName,
            String holderDocument,
            String bankCode,
            String branch,
            String accountNumber,
            AccountType accountType,
            AccountPlan accountPlan,
            BigDecimal balance,
            boolean active
    ) {
        this.id = id;
        this.holderName = holderName;
        this.holderDocument = holderDocument;
        this.bankCode = bankCode;
        this.branch = branch;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.accountPlan = accountPlan;
        this.balance = balance;
        this.active = active;
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