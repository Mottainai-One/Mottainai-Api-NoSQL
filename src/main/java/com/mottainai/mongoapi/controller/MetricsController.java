package com.mottainai.mongoapi.controller;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mottainai.mongoapi.dto.metrics.MetricResponse;
import com.mottainai.mongoapi.dto.metrics.MetricsSummaryResponse;
import com.mottainai.mongoapi.service.MetricsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Observabilidade — somente leitura + agregações")
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping
    @Operation(summary = "Lista métricas de execução de agentes, com filtros opcionais de agente e período")
    public Page<MetricResponse> list(
            @RequestParam Long empresaId,
            @RequestParam(required = false) String agent,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return metricsService.listMetrics(empresaId, agent, from, to, pageable);
    }

    @GetMapping("/summary")
    @Operation(summary = "Agregação por agente: total de execuções, custo total, latência média, "
            + "média do judgeScore (ignorando nulos) e taxa de erro")
    public MetricsSummaryResponse summary(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return metricsService.summary(empresaId, from, to);
    }
}
