package com.mottainai.mongoapi.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.mottainai.mongoapi.AbstractIntegrationTest;
import com.mottainai.mongoapi.document.AgentExecution;
import com.mottainai.mongoapi.document.Metric;

class MetricsControllerIT extends AbstractIntegrationTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long OTHER_EMPRESA_ID = 2L;

    @Test
    void listMetrics_withoutEmpresaId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/metrics"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listMetrics_returnsOnlyOwnEmpresaMetrics() throws Exception {
        mongoTemplate.save(Metric.builder().empresaId(EMPRESA_ID).agent("funcionario")
                .status("completed").latency(1.2).judgeScore(0.9).createdAt(Instant.now()).build());
        mongoTemplate.save(Metric.builder().empresaId(OTHER_EMPRESA_ID).agent("funcionario")
                .status("completed").latency(2.5).judgeScore(0.8).createdAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/metrics").param("empresaId", String.valueOf(EMPRESA_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].empresaId").value(EMPRESA_ID));
    }

    @Test
    void summary_ignoresNullJudgeScoreInAverageAndComputesErrorRate() throws Exception {
        mongoTemplate.save(Metric.builder().empresaId(EMPRESA_ID).agent("funcionario")
                .status("completed").latency(1.0).estimatedCost(0.10).judgeScore(0.8).createdAt(Instant.now()).build());
        mongoTemplate.save(Metric.builder().empresaId(EMPRESA_ID).agent("funcionario")
                .status("error").latency(2.0).estimatedCost(0.20).judgeScore(null).createdAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/metrics/summary").param("empresaId", String.valueOf(EMPRESA_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agents[0].agent").value("funcionario"))
                .andExpect(jsonPath("$.agents[0].totalExecutions").value(2))
                .andExpect(jsonPath("$.agents[0].avgJudgeScore").value(0.8))
                .andExpect(jsonPath("$.agents[0].errorRate").value(0.5));
    }

    @Test
    void summary_countsLowJudgeScoresWithoutDoubleCountingFailures() throws Exception {
        for (double score : new double[] {0.0, 0.69, 0.7, 1.0}) {
            mongoTemplate.save(Metric.builder().empresaId(EMPRESA_ID).agent("gerente")
                    .status("completed").judgeScore(score).createdAt(Instant.now()).build());
        }
        mongoTemplate.save(Metric.builder().empresaId(EMPRESA_ID).agent("gerente")
                .status("error").judgeScore(0.2).createdAt(Instant.now()).build());
        mongoTemplate.save(Metric.builder().empresaId(EMPRESA_ID).agent("gerente")
                .status("completed").createdAt(Instant.now()).build());
        mongoTemplate.save(Metric.builder().empresaId(OTHER_EMPRESA_ID).agent("gerente")
                .status("error").judgeScore(0.0).createdAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/metrics/summary").param("empresaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agents[0].totalExecutions").value(6))
                .andExpect(jsonPath("$.agents[0].avgLatency").doesNotExist())
                .andExpect(jsonPath("$.agents[0].errorRate").value(0.5));
    }

    @Test
    void listMetrics_readsRequestIdFromCurrentIaDocument() throws Exception {
        mongoTemplate.getCollection("metrics").insertOne(new org.bson.Document()
                .append("empresaId", 1).append("sessionId", "session-current")
                .append("requestId", "request-current").append("agent", "gerente")
                .append("status", "completed").append("createdAt", java.util.Date.from(Instant.now())));

        mockMvc.perform(get("/api/v1/metrics").param("empresaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestId").value("request-current"));
    }

    @Test
    void listExecutions_withoutEmpresaId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/executions"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listExecutions_returnsOnlyOwnEmpresaExecutions() throws Exception {
        mongoTemplate.save(AgentExecution.builder().empresaId(EMPRESA_ID).agent("funcionario")
                .status("completed").latency(1.5).createdAt(Instant.now()).startedAt(Instant.now()).build());
        mongoTemplate.save(AgentExecution.builder().empresaId(OTHER_EMPRESA_ID).agent("funcionario")
                .status("completed").latency(1.5).createdAt(Instant.now()).startedAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/executions").param("empresaId", String.valueOf(EMPRESA_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void listExecutions_returnsStructuredErrorAndAgentFlowFields() throws Exception {
        // Formato atual gravado pela IA Layer: error é documento, não string.
        mongoTemplate.save(AgentExecution.builder().empresaId(EMPRESA_ID).agent("unavailable")
                .requestId("request-execution")
                .skill("promocoes")
                .status("error").latency(8.07)
                .tools(List.of("rag_search", "postgres_query"))
                .executionFlow(List.of("Supervisor", "Cliente", "Resposta"))
                .error(Map.of("message", "APIStatusError"))
                .startedAt(Instant.now()).finishedAt(Instant.now()).createdAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/executions")
                        .param("empresaId", String.valueOf(EMPRESA_ID))
                        .param("status", "error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].error.message").value("APIStatusError"))
                .andExpect(jsonPath("$.content[0].requestId").value("request-execution"))
                .andExpect(jsonPath("$.content[0].skill").value("promocoes"))
                .andExpect(jsonPath("$.content[0].tools[0]").value("rag_search"))
                .andExpect(jsonPath("$.content[0].executionFlow[0]").value("Supervisor"))
                .andExpect(jsonPath("$.content[0].finishedAt").exists());
    }
}
