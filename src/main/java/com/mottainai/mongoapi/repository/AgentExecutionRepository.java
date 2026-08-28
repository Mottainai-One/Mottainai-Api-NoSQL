package com.mottainai.mongoapi.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.mottainai.mongoapi.document.AgentExecution;

public interface AgentExecutionRepository extends MongoRepository<AgentExecution, String>, AgentExecutionRepositoryCustom {
}
