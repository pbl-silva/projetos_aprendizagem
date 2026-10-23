package br.com.spbank.investimento.application.model;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
public record InvestmentPosition(UUID id,UUID applicationId,UUID customerId,UUID accountId,InvestmentProduct product,
        BigDecimal principal,LocalDate effectiveDate,@Nullable BigDecimal units,@Nullable BigDecimal unitPrice,
        @Nullable LocalDate quoteDate,@Nullable LocalDate maturityDate) {
    public InvestmentPosition {
        if(principal.signum()<=0)throw new IllegalArgumentException("Principal inválido");
        if(product.type()==InvestmentProduct.Type.FUND){
            if(units==null||units.signum()<=0||unitPrice==null||unitPrice.signum()<=0||quoteDate==null||maturityDate!=null)
                throw new IllegalArgumentException("Lote de fundo inválido");
        }else if(units!=null||unitPrice!=null||quoteDate!=null||maturityDate==null)throw new IllegalArgumentException("CDB não usa cotas");
    }
}
