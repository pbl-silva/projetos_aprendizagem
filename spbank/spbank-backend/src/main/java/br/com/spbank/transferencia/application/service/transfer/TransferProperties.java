package br.com.spbank.transferencia.application.service.transfer;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spbank.transfer")
public record TransferProperties(
        long schedulerDelay,
        Internal internal,
        Ted ted
) {

    public record Internal(
            BigDecimal maxPerOperation
    ) {
    }

    public record Ted(
            BigDecimal maxPerOperation,
            BigDecimal fee,
            int plusFreePerMonth,
            String opensAt,
            String closesAt
    ) {
    }
}