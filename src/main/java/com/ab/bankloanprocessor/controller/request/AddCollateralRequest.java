package com.ab.bankloanprocessor.controller.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AddCollateralRequest(
        @NotNull Map<String, Object> metadata
) {
}
