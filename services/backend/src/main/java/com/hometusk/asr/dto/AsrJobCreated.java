package com.hometusk.asr.dto;

import java.time.Instant;
import java.util.UUID;

public record AsrJobCreated(UUID id, String status, Instant createdAt) {}
