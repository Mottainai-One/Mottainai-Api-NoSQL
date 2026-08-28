package com.mottainai.mongoapi.dto.metrics;

import java.time.Instant;
import java.util.List;

public record MetricsSummaryResponse(
        Long empresaId,
        Instant from,
        Instant to,
        List<AgentMetricsSummary> agents
) {
}
