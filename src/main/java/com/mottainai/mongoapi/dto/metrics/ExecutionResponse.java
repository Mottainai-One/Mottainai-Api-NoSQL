package com.mottainai.mongoapi.dto.metrics;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ExecutionResponse(
        String id,
        Long empresaId,
        String sessionId,
        String requestId,
        String conversationId,
        String agent,
        String skill,
        String status,
        Double latency,
        Map<String, Double> nodeLatenciesMs,
        List<String> tools,
        List<String> executionFlow,
        Map<String, Object> error,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
) {
}
