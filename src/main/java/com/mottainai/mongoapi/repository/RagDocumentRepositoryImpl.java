package com.mottainai.mongoapi.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;

import com.mottainai.mongoapi.document.RagDocument;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RagDocumentRepositoryImpl implements RagDocumentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<RagDocument> searchByTitleText(Long empresaId, String text, Pageable pageable) {
        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingAny(text);
        TextQuery query = TextQuery.queryText(textCriteria);
        query.addCriteria(Criteria.where("empresaId").is(empresaId));
        query.sortByScore();

        long total = mongoTemplate.count(query, RagDocument.class);
        query.with(pageable);
        List<RagDocument> content = mongoTemplate.find(query, RagDocument.class);

        return new PageImpl<>(content, pageable, total);
    }
}
