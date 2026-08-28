package com.mottainai.mongoapi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mottainai.mongoapi.document.AgentExecution;

public interface AgentExecutionRepositoryCustom {

    Page<AgentExecution> search(Long empresaId, String agent, String status, Pageable pageable);
}
