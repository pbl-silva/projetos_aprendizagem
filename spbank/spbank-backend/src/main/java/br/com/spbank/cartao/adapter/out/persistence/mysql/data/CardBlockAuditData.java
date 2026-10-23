package br.com.spbank.cartao.adapter.out.persistence.mysql.data;

import br.com.spbank.cartao.application.model.CardBlockAudit;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "auditoria_bloqueio_cartao")
public class CardBlockAuditData {
    @Id @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", columnDefinition = "CHAR(36)") private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cartao_id", columnDefinition = "CHAR(36)") private UUID cardId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "responsavel_id", columnDefinition = "CHAR(36)") private UUID actorId;
    @Column(name = "acao", length = 40) private String action;
    @Column(name = "motivo", length = 240) private String reason;
    @Column(name = "ocorrida_em") private Instant occurredAt;

    protected CardBlockAuditData() {}
    public CardBlockAuditData(CardBlockAudit audit) {
        id = audit.id(); cardId = audit.cardId(); actorId = audit.actorId();
        action = audit.action(); reason = audit.reason(); occurredAt = audit.occurredAt();
    }
}
