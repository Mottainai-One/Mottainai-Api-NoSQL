package com.mottainai.mongoapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import com.mottainai.mongoapi.document.Metric;
import com.mottainai.mongoapi.dto.metrics.AgentMetricsSummary;
import com.mottainai.mongoapi.dto.metrics.MetricResponse;
import com.mottainai.mongoapi.dto.metrics.MetricsSummaryResponse;
import com.mottainai.mongoapi.mapper.MetricsMapper;
import com.mottainai.mongoapi.repository.AgentExecutionRepository;
import com.mottainai.mongoapi.repository.MetricRepository;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    private static final Long EMPRESA_ID = 1L;

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private AgentExecutionRepository agentExecutionRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        MetricsMapper mapper = Mappers.getMapper(MetricsMapper.class);
        metricsService = new MetricsService(metricRepository, agentExecutionRepository, mapper, mongoTemplate);
    }

    @Test
    void listMetrics_delegatesToRepositoryAndMapsResults() {
        Metric metric = Metric.builder().id("m-1").empresaId(EMPRESA_ID).agent("funcionario")
                .status("completed").latency(1.5).build();
        Pageable pageable = PageRequest.of(0, 20);
        when(metricRepository.search(EMPRESA_ID, "funcionario", null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(metric), pageable, 1));

        var result = metricsService.listMetrics(EMPRESA_ID, "funcionario", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        MetricResponse response = result.getContent().get(0);
        assertThat(response.agent()).isEqualTo("funcionario");
        assertThat(response.latency()).isEqualTo(1.5);
    }

    @Test
    void summary_mapsAggregationResultsIntoResponse() {
        AgentMetricsSummary funcionario = new AgentMetricsSummary("funcionario", 10L, 5.25, 1.8, 0.91, 0.1);
        AggregationResults<AgentMetricsSummary> results =
                new AggregationResults<>(List.of(funcionario), new Document());

        when(mongoTemplate.aggregate(any(Aggregation.class), eq(Metric.class), eq(AgentMetricsSummary.class)))
                .thenReturn(results);

        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        MetricsSummaryResponse summary = metricsService.summary(EMPRESA_ID, from, to);

        assertThat(summary.empresaId()).isEqualTo(EMPRESA_ID);
        assertThat(summary.agents()).hasSize(1);
        assertThat(summary.agents().get(0).agent()).isEqualTo("funcionario");
        assertThat(summary.agents().get(0).avgJudgeScore()).isEqualTo(0.91);
        assertThat(summary.agents().get(0).errorRate()).isEqualTo(0.1);
    }
}
