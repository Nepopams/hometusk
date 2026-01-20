package com.hometusk.commands.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ContinueCommandRequest(@NotNull Map<String, Object> additionalInput) {}
