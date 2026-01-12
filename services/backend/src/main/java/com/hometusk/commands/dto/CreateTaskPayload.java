package com.hometusk.commands.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateTaskPayload(String title, String description, UUID assigneeId, UUID zoneId, Instant deadline) {}
