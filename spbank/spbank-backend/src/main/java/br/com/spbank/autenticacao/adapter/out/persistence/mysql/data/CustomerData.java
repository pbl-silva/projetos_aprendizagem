package br.com.spbank.autenticacao.adapter.out.persistence.mysql.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
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

    @Column(name = "data_nascimento")
    private LocalDate birthDate;

    @Column(name = "celular")
    private String mobile;

    @Column(name = "email")
    private String email;

    @Column(name = "cep")
    private String postalCode;

    @Column(name = "logradouro")
    private String street;

    @Column(name = "numero_endereco")
    private String addressNumber;

    @Column(name = "complemento")
    private String complement;

    @Column(name = "bairro")
    private String district;

    @Column(name = "cidade")
    private String city;

    @Column(name = "uf")
    private String state;

    @Column(name = "ativo")
    private boolean active;

    protected CustomerData() {
    }

    public CustomerData(
            UUID id,
            String fullName,
            String cpf,
            LocalDate birthDate,
            String mobile,
            String email,
            String postalCode,
            String street,
            String addressNumber,
            String complement,
            String district,
            String city,
            String state,
            boolean active
    ) {
        this.id = id;
        this.fullName = fullName;
        this.cpf = cpf;
        this.birthDate = birthDate;
        this.mobile = mobile;
        this.email = email;
        this.postalCode = postalCode;
        this.street = street;
        this.addressNumber = addressNumber;
        this.complement = complement;
        this.district = district;
        this.city = city;
        this.state = state;
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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getMobile() {
        return mobile;
    }

    public String getEmail() {
        return email;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getStreet() {
        return street;
    }

    public String getAddressNumber() {
        return addressNumber;
    }

    public String getComplement() {
        return complement;
    }

    public String getDistrict() {
        return district;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public boolean isActive() {
        return active;
    }
}