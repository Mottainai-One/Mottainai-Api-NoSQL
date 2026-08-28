package com.mottainai.mongoapi.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mottainai.mongoapi.dto.metrics.ExecutionResponse;
import com.mottainai.mongoapi.service.MetricsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
@Tag(name = "Executions", description = "Somente leitura — execuções de agentes registradas pela IA Layer")
public class ExecutionController {

    private final MetricsService metricsService;

    @GetMapping
    @Operation(summary = "Lista execuções de agentes, com filtros opcionais de agente e status")
    public Page<ExecutionResponse> list(
            @RequestParam Long empresaId,
            @RequestParam(required = false) String agent,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return metricsService.listExecutions(empresaId, agent, status, pageable);
    }
}
