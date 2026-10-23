package br.com.spbank.cartao.application.service;

import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.model.CustomerPlan;
import java.math.*;
import java.util.*;

public final class CardFinancePolicy {
    public static final BigDecimal ZERO=new BigDecimal("0.00");
    private CardFinancePolicy(){}
    public static BigDecimal money(BigDecimal value){
        if(value==null||value.signum()<0||value.scale()>2||value.precision()-value.scale()>17)throw error("AMOUNT");
        return value.setScale(2,RoundingMode.UNNECESSARY);
    }
    public static BigDecimal positive(BigDecimal value){value=money(value);if(value.signum()==0)throw error("AMOUNT");return value;}
    public static BigDecimal round(BigDecimal value){return value.setScale(2,RoundingMode.HALF_UP);}
    public static BigDecimal purchaseRate(CustomerPlan plan){return plan==CustomerPlan.STANDARD?new BigDecimal("0.02"):BigDecimal.ZERO;}
    public static BigDecimal agreementRate(CustomerPlan plan){return new BigDecimal(plan==CustomerPlan.STANDARD?"0.03":"0.015");}
    public static BigDecimal interest(BigDecimal principal,int count,CustomerPlan plan,boolean agreement){
        int max=plan==CustomerPlan.STANDARD?6:12;
        if(count<(agreement?2:1)||count>max)throw error("INSTALLMENTS");
        return count==1?ZERO:round(principal.multiply(agreement?agreementRate(plan):purchaseRate(plan)).multiply(BigDecimal.valueOf(agreement?count-1:count)));
    }
    public record Part(BigDecimal principal,BigDecimal interest){public BigDecimal total(){return principal.add(interest);}}
    public static List<Part> split(BigDecimal principal,BigDecimal interest,int count,BigDecimal minimum){
        BigDecimal total=principal.add(interest), regular=total.divide(BigDecimal.valueOf(count),2,RoundingMode.DOWN);
        var result=new ArrayList<Part>();BigDecimal usedTotal=ZERO,usedInterest=ZERO;
        for(int index=0;index<count;index++){
            var amount=index==count-1?total.subtract(usedTotal):regular;
            if(amount.compareTo(minimum)<0)throw error("MINIMUM");
            var share=index==count-1?interest.subtract(usedInterest):interest.multiply(amount).divide(total,2,RoundingMode.DOWN);
            result.add(new Part(amount.subtract(share),share));usedTotal=usedTotal.add(amount);usedInterest=usedInterest.add(share);
        }return List.copyOf(result);
    }
    public static BusinessException error(String code){return new BusinessException("CARD_FINANCE_"+code,"card.finance."+code.toLowerCase(Locale.ROOT));}
}
