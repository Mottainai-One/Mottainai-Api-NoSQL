package com.mottainai.mongoapi.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mottainai.mongoapi.document.Conversation;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConversationRepositoryImpl implements ConversationRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Conversation> search(Long empresaId, Long usuarioId, String status, Pageable pageable) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("empresaId").is(empresaId));
        if (usuarioId != null) {
            criteriaList.add(Criteria.where("usuarioId").is(usuarioId));
        }
        if (status != null && !status.isBlank()) {
            criteriaList.add(Criteria.where("status").is(status));
        }

        Criteria criteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = Query.query(criteria);

        long total = mongoTemplate.count(query, Conversation.class);
        query.with(pageable);
        List<Conversation> content = mongoTemplate.find(query, Conversation.class);

        return new PageImpl<>(content, pageable, total);
    }
}
