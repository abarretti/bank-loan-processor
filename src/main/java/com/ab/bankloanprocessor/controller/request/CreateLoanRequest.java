package com.ab.bankloanprocessor.controller.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateLoanRequest(
        @NotNull
        UUID debtorId,
        @NotNull
        UUID creditorId,
        @NotNull
        String idempotencyKey,
        @NotNull @Positive
        BigDecimal amount) {
}
