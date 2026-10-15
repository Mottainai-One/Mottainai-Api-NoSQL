package com.mottainai.mongoapi.dto.metrics;

public record AgentMetricsSummary(
        String agent,
        long totalExecutions,
        double totalCost,
        Double avgLatency,
        Double avgJudgeScore,
        double errorRate
) {
}
