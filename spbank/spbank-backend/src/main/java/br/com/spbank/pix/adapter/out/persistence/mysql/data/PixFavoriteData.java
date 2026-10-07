package br.com.spbank.pix.adapter.out.persistence.mysql.data;

import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixKeyTypeConverter;
import br.com.spbank.pix.adapter.out.persistence.mysql.converter.PixRoutingConverter;
import br.com.spbank.pix.application.model.PixKeyType;
import br.com.spbank.pix.application.model.PixRouting;

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
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "conta_origem_id",
        columnDefinition = "CHAR(36)"
    )
    private UUID sourceAccountId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "chave_pix_id",
        columnDefinition = "CHAR(36)"
    )
    private UUID pixKeyId;

    @Column(name = "valor_chave_normalizado")
    private String normalizedKey;

    @Convert(converter = PixRoutingConverter.class)
    @Column(name = "escopo_destino")
    private PixRouting routing;

    @Column(name = "nome_destinatario")
    private String recipientName;

    @Column(name = "codigo_banco_destino")
    private String bankCode;

    @Column(name = "nome_banco_destino")
    private String bankName;

    @Column(
        name = "documento_destinatario_mascarado"
    )
    private String maskedRecipientDocument;

    @Column(name = "chave_mascarada")
    private String maskedKey;

    @Convert(converter = PixKeyTypeConverter.class)
    @Column(name = "tipo_chave")
    private PixKeyType keyType;

    @Column(name = "criado_em")
    private Instant createdAt;

    protected PixFavoriteData() {
    }

    public PixFavoriteData(
        UUID id,
        UUID sourceAccountId,
        UUID pixKeyId,
        String normalizedKey,
        PixRouting routing,
        String recipientName,
        String bankCode,
        String bankName,
        String maskedRecipientDocument,
        String maskedKey,
        PixKeyType keyType,
        Instant createdAt
    ) {
        this.id = id;
        this.sourceAccountId = sourceAccountId;
        this.pixKeyId = pixKeyId;
        this.normalizedKey = normalizedKey;
        this.routing = routing;
        this.recipientName = recipientName;
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.maskedRecipientDocument =
            maskedRecipientDocument;
        this.maskedKey = maskedKey;
        this.keyType = keyType;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getPixKeyId() {
        return pixKeyId;
    }

    public String getNormalizedKey() {
        return normalizedKey;
    }

    public PixRouting getRouting() {
        return routing;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public String getMaskedRecipientDocument() {
        return maskedRecipientDocument;
    }

    public String getMaskedKey() {
        return maskedKey;
    }

    public PixKeyType getKeyType() {
        return keyType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}