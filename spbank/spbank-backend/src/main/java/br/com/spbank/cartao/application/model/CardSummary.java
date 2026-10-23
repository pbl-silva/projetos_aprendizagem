package br.com.spbank.cartao.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CardSummary(UUID id, CardBrand brand, String lastFour, CardState state,
        boolean pinBlocked, boolean voluntaryBlocked, boolean administrativeBlocked,
        String voluntaryReason, String administrativeReason, LocalDate expiresOn,
        BigDecimal allocated, BigDecimal committed, BigDecimal available, BigDecimal excess) {
}
