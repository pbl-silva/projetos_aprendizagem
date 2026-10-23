package br.com.spbank.investimento.application.service;
import br.com.spbank.investimento.application.model.*;
import br.com.spbank.investimento.application.port.out.InvestmentPersistence;
import br.com.spbank.investimento.application.port.out.InvestmentReferenceData;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public final class InvestmentValuationCalculator {
    public static final String ALGORITHM_VERSION="INV4-1";
    private static final MathContext PRECISION=MathContext.DECIMAL128;
    private final InvestmentPersistence investments;
    private final InvestmentReferenceData reference;
    private final InvestmentBusinessCalendar calendar;
    public InvestmentValuationCalculator(InvestmentPersistence investments,InvestmentReferenceData reference,InvestmentBusinessCalendar calendar){
        this.investments=investments;this.reference=reference;this.calendar=calendar;
    }
    public InvestmentValuation calculate(InvestmentPosition position,LocalDate asOf){
        if(asOf.isBefore(position.effectiveDate()))return unavailable(position,asOf,"INVESTMENT_VALUATION_DATE");
        if(position.product().type()==InvestmentProduct.Type.FUND)return fund(position,asOf);
        return cdb(position,asOf);
    }
    private InvestmentValuation fund(InvestmentPosition position,LocalDate asOf){
        var quote=investments.quote(position.product().id(),asOf);
        if(quote.isEmpty())return unavailable(position,asOf,"INVESTMENT_QUOTE_MISSING");
        var current=quote.orElseThrow();
        if(!current.productId().equals(position.product().id())||current.date().isAfter(asOf)||current.date().isBefore(Objects.requireNonNull(position.quoteDate())))
            return unavailable(position,asOf,"INVESTMENT_QUOTE_DATE");
        var gross=money(Objects.requireNonNull(position.units()).multiply(current.price(),PRECISION));
        return available(position,asOf,gross,current.date(),current,List.of(),0);
    }
    private InvestmentValuation cdb(InvestmentPosition position,LocalDate asOf){
        var maturity=Objects.requireNonNull(position.maturityDate());
        var through=asOf.isAfter(maturity)?maturity:asOf;
        var dates=calendar.businessDates(position.effectiveDate(),through);
        if(dates.isEmpty())return available(position,asOf,money(position.principal()),position.effectiveDate(),null,List.of(),0);
        var periods=reference.cdiPeriods(dates.getFirst(),dates.getLast());
        var used=new LinkedHashSet<InvestmentCdiPeriod>();
        var percentage=Objects.requireNonNull(position.product().cdb()).cdiPercent().movePointLeft(2);
        var balance=position.principal();
        // Application day excluded; reference day included. Only business days accrue.
        for(var date:dates){
            InvestmentCdiPeriod selected=null;
            for(var period:periods)if(period.covers(date)){
                if(selected!=null)return unavailable(position,asOf,"INVESTMENT_CDI_OVERLAP");selected=period;
            }
            if(selected==null)return unavailable(position,asOf,"INVESTMENT_CDI_MISSING");
            var factor=BigDecimal.ONE.add(selected.dailyFactor().subtract(BigDecimal.ONE).multiply(percentage,PRECISION),PRECISION);
            if(factor.signum()<=0)return unavailable(position,asOf,"INVESTMENT_CDI_INVALID");
            balance=balance.multiply(factor,PRECISION);used.add(selected);
        }
        return available(position,asOf,money(balance),dates.getLast(),null,List.copyOf(used),dates.size());
    }
    private InvestmentValuation available(InvestmentPosition position,LocalDate asOf,BigDecimal gross,LocalDate through,
            InvestmentQuote quote,List<InvestmentCdiPeriod> periods,int days){
        if(gross.signum()<0||gross.precision()-gross.scale()>17)return unavailable(position,asOf,"INVESTMENT_VALUATION_OVERFLOW");
        var earnings=money(gross.subtract(position.principal()));
        var signature=hash(position+"|"+asOf+"|"+gross.toPlainString()+"|"+through+"|"+quote+"|"+periods);
        return new InvestmentValuation(position,asOf,InvestmentValuation.Status.AVAILABLE,gross,earnings,through,quote,periods,days,null,signature,ALGORITHM_VERSION);
    }
    private InvestmentValuation unavailable(InvestmentPosition position,LocalDate asOf,String code){
        return new InvestmentValuation(position,asOf,InvestmentValuation.Status.DATA_UNAVAILABLE,null,null,null,null,List.of(),0,code,
            hash(position+"|"+asOf+"|"+code),ALGORITHM_VERSION);
    }
    private static BigDecimal money(BigDecimal value){return value.setScale(2,RoundingMode.HALF_EVEN);}
    private static String hash(String input){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((ALGORITHM_VERSION+"|"+input).getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException("SHA-256 indisponível",ex);}}
}
