package br.com.spbank.investimento.application.service;
import br.com.spbank.investimento.application.port.out.InvestmentPersistence;
import java.time.*;
import org.springframework.stereotype.Component;
@Component
public final class InvestmentBusinessCalendar {
    private final InvestmentPersistence persistence;
    public InvestmentBusinessCalendar(InvestmentPersistence persistence){this.persistence=persistence;}
    public LocalDate today(Clock clock){return LocalDate.ofInstant(clock.instant(),ZoneId.of("America/Sao_Paulo"));}
    public boolean businessDay(LocalDate date){return date.getDayOfWeek()!=DayOfWeek.SATURDAY&&date.getDayOfWeek()!=DayOfWeek.SUNDAY&&!persistence.holiday(date);}
    public java.util.List<LocalDate> businessDates(LocalDate fromExclusive,LocalDate throughInclusive){
        if(!throughInclusive.isAfter(fromExclusive))return java.util.List.of();
        var holidays=persistence.holidays(fromExclusive.plusDays(1),throughInclusive);
        return fromExclusive.plusDays(1).datesUntil(throughInclusive.plusDays(1)).filter(date->date.getDayOfWeek()!=DayOfWeek.SATURDAY&&date.getDayOfWeek()!=DayOfWeek.SUNDAY&&!holidays.contains(date)).toList();
    }
    public LocalDate nextOrSame(LocalDate date){for(int i=0;i<370;i++,date=date.plusDays(1))if(businessDay(date))return date;throw new IllegalStateException("Calendário indisponível");}
}
