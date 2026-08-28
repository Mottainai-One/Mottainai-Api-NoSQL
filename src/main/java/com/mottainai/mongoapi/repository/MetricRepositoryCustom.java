package com.mottainai.mongoapi.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mottainai.mongoapi.document.Metric;

public interface MetricRepositoryCustom {

    Page<Metric> search(Long empresaId, String agent, Instant from, Instant to, Pageable pageable);
}
