package com.mottainai.mongoapi.dto.metrics;

import java.time.Instant;
import java.util.Map;

public record MetricResponse(
        String id,
        Long empresaId,
        String sessionId,
        String requestId,
        String conversationId,
        String agent,
        String skill,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Double latency,
        Double estimatedCost,
        CostReferenceResponse costReference,
        Double judgeScore,
        String status,
        Map<String, Double> nodeLatenciesMs,
        Instant createdAt
) {
}
