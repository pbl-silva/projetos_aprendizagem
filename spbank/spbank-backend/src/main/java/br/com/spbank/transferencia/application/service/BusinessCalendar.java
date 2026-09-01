package br.com.spbank.transferencia.application.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public final class BusinessCalendar {

    private final ZoneId zone;
    private final LocalTime tedOpensAt;
    private final LocalTime tedClosesAt;

    private final Set<LocalDate> holidays = Set.of();

    public BusinessCalendar(
            @Value("${spbank.zone}") String zone,
            TransferProperties properties
    ) {

        this.zone = ZoneId.of(zone);

        this.tedOpensAt =
                LocalTime.parse(
                        properties.ted().opensAt()
                );

        this.tedClosesAt =
                LocalTime.parse(
                        properties.ted().closesAt()
                );
    }

    public LocalDate today(Instant instant) {

        return instant
                .atZone(zone)
                .toLocalDate();
    }

    public boolean isBusinessDay(LocalDate date) {

        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY
                && !holidays.contains(date);
    }

    public LocalDate normalizeTedScheduledDate(
            LocalDate requested
    ) {

        LocalDate date = requested;

        while (!isBusinessDay(date)) {
            date = date.plusDays(1);
        }

        return date;
    }

    public boolean isTedWindowOpen(
            Instant instant
    ) {

        ZonedDateTime local =
                instant.atZone(zone);

        LocalTime time =
                local.toLocalTime();

        return isBusinessDay(
                    local.toLocalDate()
                )
                && !time.isBefore(tedOpensAt)
                && !time.isAfter(tedClosesAt);
    }

    public LocalDate effectiveTedDate(
            LocalDate requested,
            Instant instant
    ) {

        if (requested != null) {
            return normalizeTedScheduledDate(
                    requested
            );
        }

        ZonedDateTime local =
                instant.atZone(zone);

        LocalDate today =
                local.toLocalDate();

        if (isTedWindowOpen(instant)) {
            return today;
        }

        return nextBusinessDay(
                today.plusDays(1)
        );
    }

    public Instant monthStart(
            Instant instant
    ) {

        ZonedDateTime local =
                instant.atZone(zone);

        return local
                .withDayOfMonth(1)
                .toLocalDate()
                .atStartOfDay(zone)
                .toInstant();
    }

    public Instant nextMonthStart(
            Instant instant
    ) {

        ZonedDateTime local =
                instant.atZone(zone);

        return local
                .withDayOfMonth(1)
                .toLocalDate()
                .plusMonths(1)
                .atStartOfDay(zone)
                .toInstant();
    }

    private LocalDate nextBusinessDay(
            LocalDate date
    ) {

        LocalDate candidate = date;

        while (!isBusinessDay(candidate)) {
            candidate =
                    candidate.plusDays(1);
        }

        return candidate;
    }
}