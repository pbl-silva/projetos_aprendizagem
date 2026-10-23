package br.com.spbank.investimento.application.model;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
public record InvestmentProduct(UUID id,String name,Type type,BigDecimal minimumApplication,
        BigDecimal minimumRemaining,String risk,boolean active,@Nullable FundTerms fund,@Nullable CdbTerms cdb) {
    public enum Type { FUND,CDB }
    public record FundTerms(int applicationDays,int redemptionDays,int paymentDays) {
        public FundTerms { if(applicationDays!=0||redemptionDays!=0||paymentDays!=1)throw new IllegalArgumentException("Prazos do fundo inválidos"); }
    }
    public record CdbTerms(BigDecimal cdiPercent,int termDays,String liquidity) {
        public CdbTerms { if(cdiPercent.signum()<=0||termDays<=0||!liquidity.equals("AT_MATURITY"))throw new IllegalArgumentException("Termos do CDB inválidos"); }
    }
    public InvestmentProduct {
        Objects.requireNonNull(id);Objects.requireNonNull(name);Objects.requireNonNull(type);Objects.requireNonNull(risk);
        if(minimumApplication.signum()<=0||minimumRemaining.signum()<0||minimumApplication.scale()>2||minimumRemaining.scale()>2)
            throw new IllegalArgumentException("Mínimos inválidos");
        if(type==Type.FUND?(fund==null||cdb!=null):(cdb==null||fund!=null))throw new IllegalArgumentException("Produto híbrido inválido");
    }
    public boolean fgcEligible(){return type==Type.CDB;}
}
