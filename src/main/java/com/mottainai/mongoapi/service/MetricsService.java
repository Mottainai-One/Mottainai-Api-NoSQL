package com.mottainai.mongoapi.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.mottainai.mongoapi.document.Metric;
import com.mottainai.mongoapi.dto.metrics.AgentMetricsSummary;
import com.mottainai.mongoapi.dto.metrics.ExecutionResponse;
import com.mottainai.mongoapi.dto.metrics.MetricResponse;
import com.mottainai.mongoapi.dto.metrics.MetricsSummaryResponse;
import com.mottainai.mongoapi.mapper.MetricsMapper;
import com.mottainai.mongoapi.repository.AgentExecutionRepository;
import com.mottainai.mongoapi.repository.MetricRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private static final String COMPLETED_STATUS = "completed";

    private final MetricRepository metricRepository;
    private final AgentExecutionRepository agentExecutionRepository;
    private final MetricsMapper mapper;
    private final MongoTemplate mongoTemplate;

    public Page<MetricResponse> listMetrics(Long empresaId, String agent, Instant from, Instant to, Pageable pageable) {
        return metricRepository.search(empresaId, agent, from, to, pageable).map(mapper::toResponse);
    }

    public Page<ExecutionResponse> listExecutions(Long empresaId, String agent, String status, Pageable pageable) {
        return agentExecutionRepository.search(empresaId, agent, status, pageable).map(mapper::toResponse);
    }

    /**
     * Agregação nativa do MongoDB, isolada nesta service (repositórios permanecem só com
     * consultas simples/paginadas). judgeScore nulo (execução que falhou antes do Juiz) é
     * ignorado tanto na soma quanto no denominador, pois $avg do Mongo descarta valores não
     * numéricos automaticamente. errorRate considera "erro" qualquer status diferente de
     * "completed".
     */
    public MetricsSummaryResponse summary(Long empresaId, Instant from, Instant to) {
        Criteria matchCriteria = Criteria.where("empresaId").is(empresaId);
        if (from != null || to != null) {
            Criteria dateCriteria = Criteria.where("createdAt");
            if (from != null) {
                dateCriteria = dateCriteria.gte(from);
            }
            if (to != null) {
                dateCriteria = dateCriteria.lte(to);
            }
            matchCriteria = matchCriteria.andOperator(dateCriteria);
        }

        MatchOperation match = Aggregation.match(matchCriteria);

        GroupOperation group = Aggregation.group("agent")
                .count().as("totalExecutions")
                .sum("estimatedCost").as("totalCost")
                .avg("latency").as("avgLatency")
                .avg("judgeScore").as("avgJudgeScore")
                .sum(ConditionalOperators.when(Criteria.where("status").ne(COMPLETED_STATUS)).then(1).otherwise(0))
                .as("errorCount");

        ProjectionOperation project = Aggregation.project("totalExecutions", "totalCost", "avgLatency", "avgJudgeScore")
                .and("_id").as("agent")
                .andExpression("errorCount / totalExecutions").as("errorRate")
                .andExclude("_id");

        Aggregation aggregation = Aggregation.newAggregation(match, group, project);
        AggregationResults<AgentMetricsSummary> results =
                mongoTemplate.aggregate(aggregation, Metric.class, AgentMetricsSummary.class);
        List<AgentMetricsSummary> agents = results.getMappedResults();

        return new MetricsSummaryResponse(empresaId, from, to, agents);
    }
}
