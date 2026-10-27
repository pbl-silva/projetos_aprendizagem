package br.com.spbank.pagamento.adapter.out.persistence.mysql;
import br.com.spbank.pagamento.application.model.PaymentModels.*;
import br.com.spbank.pagamento.application.port.out.PaymentPersistence;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
@Repository
public class PaymentMysqlPersistence implements PaymentPersistence {
 private final JdbcTemplate db;private final JsonMapper json;
 public PaymentMysqlPersistence(JdbcTemplate db,JsonMapper json){this.db=db;this.json=json;}
 public Optional<Charge> charge(String hash){return db.query("SELECT dados FROM catalogo_cobrancas_estudo WHERE codigo_hash=?",(r,n)->json.readValue(r.getString(1),Charge.class),hash).stream().findFirst();}
 public void save(Charge c){db.update("INSERT INTO catalogo_cobrancas_estudo(codigo_hash,dados) VALUES (?,?)",c.hash(),json.writeValueAsString(c));}
 public List<Product> products(){return db.query("SELECT dados FROM catalogo_recargas_celular ORDER BY id",(r,n)->json.readValue(r.getString(1),Product.class));}
 public Optional<Product> product(UUID id){return db.query("SELECT dados FROM catalogo_recargas_celular WHERE id=?",(r,n)->json.readValue(r.getString(1),Product.class),id.toString()).stream().findFirst();}
 public List<Payment> payments(UUID id){return db.query("SELECT dados FROM pagamentos WHERE cliente_id=? ORDER BY criado_em DESC,id",(r,n)->json.readValue(r.getString(1),Payment.class),id.toString());}
 public Optional<UUID> owner(UUID id){return db.query("SELECT cliente_id FROM pagamentos WHERE id=?",(r,n)->UUID.fromString(r.getString(1)),id.toString()).stream().findFirst();}
 public List<UUID> due(LocalDate date,int limit){return db.query("SELECT id FROM pagamentos WHERE situacao='SCHEDULED' AND agendado_para<=? ORDER BY agendado_para,id LIMIT ?",(r,n)->UUID.fromString(r.getString(1)),date,limit);}
 public void save(Payment p){if(db.update("UPDATE pagamentos SET situacao=?,dados=? WHERE id=? AND cliente_id=?",p.status().name(),json.writeValueAsString(p),p.id().toString(),p.customerId().toString())>0)return;db.update("INSERT INTO pagamentos(id,cliente_id,chave,tipo,alvo_hash,situacao,agendado_para,criado_em,dados) VALUES (?,?,?,?,?,?,?,?,?)",p.id().toString(),p.customerId().toString(),p.key().toString(),p.kind(),p.targetHash(),p.status().name(),p.scheduledFor(),java.sql.Timestamp.from(p.createdAt()),json.writeValueAsString(p));}
}
