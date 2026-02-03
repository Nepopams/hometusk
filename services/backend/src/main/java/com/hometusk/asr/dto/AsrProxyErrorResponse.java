package com.hometusk.asr.dto;

import java.util.Map;

public record AsrProxyErrorResponse(String code, String message, String correlationId, Map<String, Object> details) {}
