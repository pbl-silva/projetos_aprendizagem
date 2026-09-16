package br.com.spbank.pix.adapter.out.persistence.mysql.data;

import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixDestinationScopeConverter;
import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixKeyTypeConverter;
import br.com.spbank.pix.application.model.PixDestinationScope;
import br.com.spbank.pix.application.model.PixKeyType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "favoritos_pix")
public class PixFavoriteData {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        columnDefinition = "CHAR(36)"
    )
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "conta_origem_id",
        columnDefinition = "CHAR(36)"
    )
    private UUID originAccountId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "chave_pix_id",
        columnDefinition = "CHAR(36)"
    )
    private UUID pixKeyId;

    @Column(
        name = "valor_chave_normalizado",
        length = 120
    )
    private String normalizedKeyValue;

    @Convert(
        converter =
            PixDestinationScopeConverter.class
    )
    @Column(name = "escopo_destino")
    private PixDestinationScope destinationScope;

    @Column(
        name = "nome_destinatario",
        length = 120
    )
    private String holderName;

    @Column(
        name = "chave_mascarada",
        length = 120
    )
    private String maskedKey;

    @Convert(
        converter = PixKeyTypeConverter.class
    )
    @Column(name = "tipo_chave")
    private PixKeyType keyType;

    @Column(
        name = "codigo_banco_destino",
        length = 20
    )
    private String bankCode;

    @Column(
        name = "nome_banco_destino",
        length = 120
    )
    private String bankName;

    @Column(
        name = "documento_destinatario_mascarado",
        length = 24
    )
    private String maskedDocument;

    @Column(name = "criado_em")
    private Instant createdAt;

    protected PixFavoriteData() {
    }

    public PixFavoriteData(
        UUID id,
        UUID originAccountId,
        UUID pixKeyId,
        String normalizedKeyValue,
        PixDestinationScope destinationScope,
        String holderName,
        String maskedKey,
        PixKeyType keyType,
        String bankCode,
        String bankName,
        String maskedDocument,
        Instant createdAt
    ) {
        this.id = id;
        this.originAccountId =
            originAccountId;
        this.pixKeyId = pixKeyId;
        this.normalizedKeyValue =
            normalizedKeyValue;
        this.destinationScope =
            destinationScope;
        this.holderName = holderName;
        this.maskedKey = maskedKey;
        this.keyType = keyType;
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.maskedDocument =
            maskedDocument;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOriginAccountId() {
        return originAccountId;
    }

    public UUID getPixKeyId() {
        return pixKeyId;
    }

    public String getNormalizedKeyValue() {
        return normalizedKeyValue;
    }

    public PixDestinationScope getDestinationScope() {
        return destinationScope;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getMaskedKey() {
        return maskedKey;
    }

    public PixKeyType getKeyType() {
        return keyType;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public String getMaskedDocument() {
        return maskedDocument;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}