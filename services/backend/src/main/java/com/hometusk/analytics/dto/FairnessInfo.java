package com.hometusk.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Balance score details based on Gini coefficient")
public record FairnessInfo(
        @Schema(description = "Gini coefficient (0=equal, 1=unequal)") Double gini,
        @Schema(description = "Balance score from 0 to 100") Integer balance,
        @Schema(description = "Formula used to compute balance") String formula,
        @Schema(description = "Human-readable interpretation of the balance score") String interpretation) {}
