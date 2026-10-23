package br.com.spbank.cartao.application.service;

import br.com.spbank.cartao.application.model.finance.BillingLine;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class CardBillingCalendar {
    private final ZoneId zone;
    private final Set<LocalDate> holidays;
    public CardBillingCalendar(@Value("${spbank.zone}") String zone,
            @Value("${spbank.card.holidays:}") String holidays) {
        this.zone=ZoneId.of(zone);
        this.holidays=new HashSet<>();
        for(var value:holidays.split(",")) if(!value.isBlank())this.holidays.add(LocalDate.parse(value.strip()));
    }
    public LocalDate today(Instant now){return now.atZone(zone).toLocalDate();}
    public int day(BillingLine line,YearMonth reference){
        return line.pendingDueDay()!=0 && reference.toString().compareTo(line.effectiveReference())>=0?line.pendingDueDay():line.dueDay();
    }
    public LocalDate nominal(BillingLine line,YearMonth reference){return reference.atDay(day(line,reference));}
    public Instant closing(BillingLine line,YearMonth reference){return nominal(line,reference).minusDays(7).atStartOfDay(zone).toInstant();}
    public LocalDate effective(LocalDate date){while(date.getDayOfWeek()==DayOfWeek.SATURDAY||date.getDayOfWeek()==DayOfWeek.SUNDAY||holidays.contains(date))date=date.plusDays(1);return date;}
    public YearMonth reference(BillingLine line,Instant instant){
        var month=YearMonth.from(today(instant));
        while(!instant.isBefore(closing(line,month)))month=month.plusMonths(1);
        return month;
    }
    public Instant dayEnd(LocalDate date){return date.plusDays(1).atStartOfDay(zone).toInstant();}
}
