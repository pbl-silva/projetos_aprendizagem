package br.com.spbank.investimento.adapter.out.persistence.mysql;
import br.com.spbank.investimento.application.model.InvestmentRedemption;
import br.com.spbank.investimento.application.port.out.InvestmentRedemptions;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.InvestmentRedemptionData;
import br.com.spbank.investimento.adapter.out.persistence.mysql.repository.*;
import java.util.*;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
@Repository
public class InvestmentRedemptionsMysql implements InvestmentRedemptions {
 private final InvestmentRedemptionJpaRepository rows; private final InvestmentPositionJpaRepository positions; private final JsonMapper json;
 public InvestmentRedemptionsMysql(InvestmentRedemptionJpaRepository rows,InvestmentPositionJpaRepository positions,JsonMapper json){this.rows=rows;this.positions=positions;this.json=json;}
 public List<InvestmentRedemption> list(UUID id){return rows.findByCustomerId(id).stream().map(r->json.readValue(r.payload,InvestmentRedemption.class)).toList();}
 public List<UUID> customers(){return positions.findAll().stream().map(p->p.customerId).distinct().toList();}
 public void save(InvestmentRedemption row){var data=new InvestmentRedemptionData();data.id=row.id();data.customerId=row.customerId();data.payload=json.writeValueAsString(row);rows.saveAndFlush(data);}
}
