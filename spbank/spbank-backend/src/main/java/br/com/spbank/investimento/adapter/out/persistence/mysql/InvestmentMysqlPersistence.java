package br.com.spbank.investimento.adapter.out.persistence.mysql;
import br.com.spbank.investimento.application.model.*;
import br.com.spbank.investimento.application.port.out.InvestmentPersistence;
import br.com.spbank.investimento.adapter.out.persistence.mysql.data.*;
import br.com.spbank.investimento.adapter.out.persistence.mysql.repository.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
@Repository
public class InvestmentMysqlPersistence implements InvestmentPersistence {
    private final InvestmentProductJpaRepository products;private final InvestmentQuoteJpaRepository quotes;
    private final InvestmentHolidayJpaRepository holidays;private final InvestmentApplicationJpaRepository applications;
    private final InvestmentPositionJpaRepository positions;private final InvestmentOperationJpaRepository operations;
    private final JsonMapper json;
    public InvestmentMysqlPersistence(InvestmentProductJpaRepository products,InvestmentQuoteJpaRepository quotes,InvestmentHolidayJpaRepository holidays,
        InvestmentApplicationJpaRepository applications,InvestmentPositionJpaRepository positions,InvestmentOperationJpaRepository operations,JsonMapper json){
        this.products=products;this.quotes=quotes;this.holidays=holidays;this.applications=applications;this.positions=positions;this.operations=operations;this.json=json;}
    private InvestmentProduct product(InvestmentProductData d){var type=InvestmentProduct.Type.valueOf(d.type);
        return new InvestmentProduct(d.id,d.name,type,d.minimumApplication,d.minimumRemaining,d.risk,d.active,
            type==InvestmentProduct.Type.FUND?new InvestmentProduct.FundTerms(Objects.requireNonNull(d.applicationDays),Objects.requireNonNull(d.redemptionDays),Objects.requireNonNull(d.paymentDays)):null,
            type==InvestmentProduct.Type.CDB?new InvestmentProduct.CdbTerms(Objects.requireNonNull(d.cdiPercent),Objects.requireNonNull(d.termDays),Objects.requireNonNull(d.liquidity)):null);}
    public List<InvestmentProduct> products(){return products.findByActiveTrueOrderByName().stream().map(d->product(d)).toList();}
    public Optional<InvestmentProduct> product(UUID id,boolean locked){return (locked?products.locked(id):products.findById(id)).map(d->product(d));}
    public Optional<InvestmentQuote> quote(UUID id,LocalDate through){return quotes.findFirstByProductIdAndQuoteDateLessThanEqualOrderByQuoteDateDesc(id,through).map(d->new InvestmentQuote(d.productId,d.quoteDate,d.price));}
    public boolean holiday(LocalDate date){return holidays.existsByExactDateOrMonthDay(date,date.format(DateTimeFormatter.ofPattern("MM-dd")));}
    public Set<LocalDate> holidays(LocalDate from,LocalDate through){
        var rows=holidays.findAll();var result=new HashSet<LocalDate>();
        for(var row:rows){
            if(row.exactDate!=null){var date=row.exactDate;if(!date.isBefore(from)&&!date.isAfter(through))result.add(date);}
            else if(row.monthDay!=null){var recurring=java.time.MonthDay.parse("--"+row.monthDay);
                for(int year=from.getYear();year<=through.getYear();year++)if(recurring.isValidYear(year)){var date=recurring.atYear(year);if(!date.isBefore(from)&&!date.isAfter(through))result.add(date);}}
        }return Set.copyOf(result);
    }
    private InvestmentApplication application(InvestmentApplicationData d){return new InvestmentApplication(d.id,d.customerId,d.accountId,d.productId,d.amount,d.effectiveDate,InvestmentApplication.Status.valueOf(d.status),d.requestedAt,d.processedAt,d.positionId,d.balanceAfter,d.failureCode);}
    public Optional<InvestmentApplication> application(UUID owner,UUID id,boolean locked){return (locked?applications.locked(owner,id):applications.findByCustomerIdAndId(owner,id)).map(d->application(d));}
    public Optional<UUID> owner(UUID id){return applications.findById(id).map(d->d.customerId);}
    public List<UUID> due(LocalDate through,int limit){return applications.due(through,PageRequest.of(0,limit));}
    public List<InvestmentPosition> positions(UUID owner){return positions.findByCustomerIdOrderByEffectiveDateDescId(owner).stream().map(d->new InvestmentPosition(d.id,d.applicationId,d.customerId,d.accountId,json.readValue(d.termsJson,InvestmentProduct.class),d.principal,d.effectiveDate,d.units,d.unitPrice,d.quoteDate,d.maturityDate)).toList();}
    public Optional<InvestmentOperation> operation(UUID owner,UUID key){return operations.findByCustomerIdAndOperationKey(owner,key).map(d->new InvestmentOperation(d.id,d.customerId,d.operationKey,d.fingerprint,d.responseJson,d.createdAt));}
    public void save(InvestmentApplication row){var d=new InvestmentApplicationData();d.id=row.id();d.customerId=row.customerId();d.accountId=row.accountId();d.productId=row.productId();d.amount=row.amount();d.effectiveDate=row.effectiveDate();d.status=row.status().name();d.requestedAt=row.requestedAt();d.processedAt=row.processedAt();d.positionId=row.positionId();d.balanceAfter=row.balanceAfter();d.failureCode=row.failureCode();applications.saveAndFlush(d);}
    public void save(InvestmentPosition row){var d=new InvestmentPositionData();d.id=row.id();d.applicationId=row.applicationId();d.customerId=row.customerId();d.accountId=row.accountId();d.productId=row.product().id();d.type=row.product().type().name();d.termsJson=json.writeValueAsString(row.product());d.principal=row.principal();d.effectiveDate=row.effectiveDate();d.units=row.units();d.unitPrice=row.unitPrice();d.quoteDate=row.quoteDate();d.maturityDate=row.maturityDate();positions.saveAndFlush(d);}
    public void save(InvestmentOperation row){var d=new InvestmentOperationData();d.id=row.id();d.customerId=row.customerId();d.operationKey=row.key();d.fingerprint=row.fingerprint();d.responseJson=row.responseJson();d.createdAt=row.createdAt();operations.saveAndFlush(d);}
}
