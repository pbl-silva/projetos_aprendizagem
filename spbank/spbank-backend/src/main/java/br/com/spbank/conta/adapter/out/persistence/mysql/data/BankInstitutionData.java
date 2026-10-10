
package br.com.spbank.conta.adapter.out.persistence.mysql.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "instituicoes_bancarias")
public class BankInstitutionData {

    @Id
    @Column(
            name = "codigo_estudo",
            length = 5,
            columnDefinition = "CHAR(5)"
    )
    private String studyCode;

    @Column(
            name = "nome",
            nullable = false,
            length = 120
    )
    private String name;

    @Column(
            name = "interna",
            nullable = false
    )
    private boolean internal;

    @Column(
            name = "ativa",
            nullable = false
    )
    private boolean active;

    protected BankInstitutionData() {
    }

    public String getStudyCode() {
        return studyCode;
    }

    public String getName() {
        return name;
    }

    public boolean isInternal() {
        return internal;
    }

    public boolean isActive() {
        return active;
    }
}
