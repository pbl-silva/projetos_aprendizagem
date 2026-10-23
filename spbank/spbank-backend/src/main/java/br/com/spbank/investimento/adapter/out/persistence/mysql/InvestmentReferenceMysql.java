package br.com.spbank.investimento.adapter.out.persistence.mysql;
import br.com.spbank.investimento.application.model.*;
import br.com.spbank.investimento.application.port.out.InvestmentReferenceData;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.InvestmentValuationData;
import br.com.spbank.investimento.adapter.out.persistence.mysql.repository.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
@Repository
public class InvestmentReferenceMysql implements InvestmentReferenceData {
    private final InvestmentCdiPeriodJpaRepository cdi;
    private final InvestmentPositionJpaRepository positions;
    private final InvestmentValuationJpaRepository valuations;
    private final JsonMapper json;
    public InvestmentReferenceMysql(InvestmentCdiPeriodJpaRepository cdi,InvestmentPositionJpaRepository positions,InvestmentValuationJpaRepository valuations,JsonMapper json){this.cdi=cdi;this.positions=positions;this.valuations=valuations;this.json=json;}
    public List<InvestmentCdiPeriod> cdiPeriods(LocalDate from,LocalDate through){return cdi.overlapping(from,through).stream().map(r->new InvestmentCdiPeriod(r.validFrom,r.validThrough,r.dailyFactor,r.businessDaysBasis,r.sourceVersion)).toList();}
    public List<UUID> customersWithPositions(){return positions.customersWithPositions();}
    public void saveValuation(InvestmentValuation value){var row=new InvestmentValuationData();
        row.id=UUID.nameUUIDFromBytes(value.calculationFingerprint().getBytes(StandardCharsets.UTF_8));
        row.positionId=value.position().id();row.customerId=value.position().customerId();row.referenceDate=value.referenceDate();row.status=value.status().name();
        row.grossValue=value.grossValue();row.grossEarnings=value.grossEarnings();row.calculatedThrough=value.calculatedThrough();row.calculationFingerprint=value.calculationFingerprint();row.algorithmVersion=value.algorithmVersion();
        row.responseJson=json.writeValueAsString(Objects.requireNonNull(value));valuations.saveAndFlush(row);
    }
}
