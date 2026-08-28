package com.mottainai.mongoapi.dto.metrics;

public record AgentMetricsSummary(
        String agent,
        long totalExecutions,
        double totalCost,
        double avgLatency,
        Double avgJudgeScore,
        double errorRate
) {
}
