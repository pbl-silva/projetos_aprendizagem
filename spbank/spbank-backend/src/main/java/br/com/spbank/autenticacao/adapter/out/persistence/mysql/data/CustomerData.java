package br.com.spbank.autenticacao.adapter.out.persistence.mysql.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "clientes")
public class CustomerData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(name = "nome_completo")
    private String fullName;

    @Column(name = "cpf")
    private String cpf;

    @Column(name = "ativo")
    private boolean active;

    protected CustomerData() {
    }

    public CustomerData(
            UUID id,
            String fullName,
            String cpf,
            boolean active
    ) {
        this.id = id;
        this.fullName = fullName;
        this.cpf = cpf;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCpf() {
        return cpf;
    }

    public boolean isActive() {
        return active;
    }
}