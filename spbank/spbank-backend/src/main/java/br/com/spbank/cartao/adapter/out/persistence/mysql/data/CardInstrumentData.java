package br.com.spbank.cartao.adapter.out.persistence.mysql.data;

import br.com.spbank.cartao.application.model.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "cartoes")
public class CardInstrumentData {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cliente_id", columnDefinition = "CHAR(36)")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "bandeira", columnDefinition = "VARCHAR(20)")
    private CardBrand brand;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cartao_anterior_id", columnDefinition = "CHAR(36)")
    private UUID previousCardId;

    @Column(name = "nome_impresso", columnDefinition = "VARCHAR(160)")
    private String holderName;

    @Column(name = "final_numero", columnDefinition = "CHAR(4)")
    private String lastFour;

    @Column(name = "hash_numero", columnDefinition = "CHAR(64)")
    private String panFingerprint;

    @Column(name = "numero_cifrado", columnDefinition = "VARCHAR(256)")
    private String encryptedPan;

    @Column(name = "cvv_cifrado", columnDefinition = "VARCHAR(256)")
    private String encryptedCvv;

    @Column(name = "hash_pin", columnDefinition = "VARCHAR(256)")
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "situacao", columnDefinition = "VARCHAR(24)")
    private CardState state;

    @Column(name = "erros_pin", columnDefinition = "INT")
    private int pinFailures;

    @Column(name = "bloqueio_pin", columnDefinition = "BOOLEAN")
    private boolean pinBlocked;

    @Column(name = "bloqueio_voluntario", columnDefinition = "BOOLEAN")
    private boolean voluntaryBlocked;

    @Column(name = "bloqueio_administrativo", columnDefinition = "BOOLEAN")
    private boolean administrativeBlocked;

    @Column(name = "motivo_voluntario", columnDefinition = "VARCHAR(240)")
    private String voluntaryReason;

    @Column(name = "motivo_administrativo", columnDefinition = "VARCHAR(240)")
    private String administrativeReason;

    @Column(name = "valida_ate", columnDefinition = "DATE")
    private LocalDate expiresOn;

    @Column(name = "criada_em", columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    protected CardInstrumentData() {}

    public CardInstrumentData(CardInstrument card) {
        this.id = card.id();
        this.customerId = card.customerId();
        this.brand = card.brand();
        this.previousCardId = card.previousCardId();
        this.holderName = card.holderName();
        this.lastFour = card.lastFour();
        this.panFingerprint = card.panFingerprint();
        this.encryptedPan = card.encryptedPan();
        this.encryptedCvv = card.encryptedCvv();
        this.pinHash = card.pinHash();
        this.state = card.state();
        this.pinFailures = card.pinFailures();
        this.pinBlocked = card.pinBlocked();
        this.voluntaryBlocked = card.voluntaryBlocked();
        this.administrativeBlocked = card.administrativeBlocked();
        this.voluntaryReason = card.voluntaryReason();
        this.administrativeReason = card.administrativeReason();
        this.expiresOn = card.expiresOn();
        this.createdAt = card.createdAt();
    }

    public CardInstrument toDomain() {
        return new CardInstrument(id, customerId, brand, previousCardId, holderName, lastFour, panFingerprint, encryptedPan, encryptedCvv, pinHash, state, pinFailures, pinBlocked, voluntaryBlocked, administrativeBlocked, voluntaryReason, administrativeReason, expiresOn, createdAt);
    }
}
