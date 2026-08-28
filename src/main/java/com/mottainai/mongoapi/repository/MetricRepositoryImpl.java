package com.mottainai.mongoapi.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mottainai.mongoapi.document.Metric;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MetricRepositoryImpl implements MetricRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Metric> search(Long empresaId, String agent, Instant from, Instant to, Pageable pageable) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("empresaId").is(empresaId));
        if (agent != null && !agent.isBlank()) {
            criteriaList.add(Criteria.where("agent").is(agent));
        }
        if (from != null || to != null) {
            Criteria dateCriteria = Criteria.where("createdAt");
            if (from != null) {
                dateCriteria = dateCriteria.gte(from);
            }
            if (to != null) {
                dateCriteria = dateCriteria.lte(to);
            }
            criteriaList.add(dateCriteria);
        }

        Criteria criteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = Query.query(criteria);

        long total = mongoTemplate.count(query, Metric.class);
        query.with(pageable);
        List<Metric> content = mongoTemplate.find(query, Metric.class);

        return new PageImpl<>(content, pageable, total);
    }
}
