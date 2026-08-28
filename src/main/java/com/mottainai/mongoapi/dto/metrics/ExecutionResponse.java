package com.mottainai.mongoapi.dto.metrics;

import java.time.Instant;
import java.util.Map;

public record ExecutionResponse(
        String id,
        Long empresaId,
        String sessionId,
        String agent,
        String status,
        Double latency,
        Map<String, Double> nodeLatenciesMs,
        String error,
        Instant startedAt,
        Instant createdAt
) {
}
