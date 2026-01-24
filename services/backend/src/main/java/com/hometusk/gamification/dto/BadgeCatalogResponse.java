package com.hometusk.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Badge catalog with earned status")
public record BadgeCatalogResponse(@Schema(description = "Badges") List<BadgeDto> badges) {}
