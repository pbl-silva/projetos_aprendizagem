package com.biblioteca.biblioteca_api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public Clock fixedClock() {
        // 2026-06-19T00:00:00 -03:00 -> 2026-06-19T03:00:00Z
        Instant instant = Instant.parse("2026-06-19T03:00:00Z");
        return Clock.fixed(instant, ZoneId.of("America/Sao_Paulo"));
    }
}