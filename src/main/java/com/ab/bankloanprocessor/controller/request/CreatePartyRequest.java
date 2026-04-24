package com.ab.bankloanprocessor.controller.request;

import com.ab.bankloanprocessor.domain.PartyRole;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreatePartyRequest(
        @NotNull
        PartyRole role,
        @NotNull
        Map<String, Object> metadata) {
}
