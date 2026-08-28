package com.mottainai.mongoapi.mapper;

import org.mapstruct.Mapper;

import com.mottainai.mongoapi.document.AgentExecution;
import com.mottainai.mongoapi.document.Metric;
import com.mottainai.mongoapi.dto.metrics.ExecutionResponse;
import com.mottainai.mongoapi.dto.metrics.MetricResponse;

@Mapper(componentModel = "spring")
public interface MetricsMapper {

    MetricResponse toResponse(Metric metric);

    ExecutionResponse toResponse(AgentExecution execution);
}
