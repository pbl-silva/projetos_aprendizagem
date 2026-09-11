package br.com.spbank.pix.application.service;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spbank.pix")
public record PixProperties(
    long schedulerDelay,
    BigDecimal maxPerOperation,
    BigDecimal favoriteRequiredAbove,
    String zone
) {
}