package com.mottainai.mongoapi.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mottainai.mongoapi.document.AgentExecution;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AgentExecutionRepositoryImpl implements AgentExecutionRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<AgentExecution> search(Long empresaId, String agent, String status, Pageable pageable) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("empresaId").is(empresaId));
        if (agent != null && !agent.isBlank()) {
            criteriaList.add(Criteria.where("agent").is(agent));
        }
        if (status != null && !status.isBlank()) {
            criteriaList.add(Criteria.where("status").is(status));
        }

        Criteria criteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = Query.query(criteria);

        long total = mongoTemplate.count(query, AgentExecution.class);
        query.with(pageable);
        List<AgentExecution> content = mongoTemplate.find(query, AgentExecution.class);

        return new PageImpl<>(content, pageable, total);
    }
}
