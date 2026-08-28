package com.mottainai.mongoapi.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;

import com.mottainai.mongoapi.document.RagChunk;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RagChunkRepositoryImpl implements RagChunkRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<RagChunk> searchByTextInDocuments(String text, Collection<String> documentIds, Pageable pageable) {
        if (documentIds.isEmpty()) {
            return Page.empty(pageable);
        }

        TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matchingAny(text);
        TextQuery query = TextQuery.queryText(textCriteria);
        query.addCriteria(Criteria.where("documentId").in(documentIds));
        query.sortByScore();

        long total = mongoTemplate.count(query, RagChunk.class);
        query.with(pageable);
        List<RagChunk> content = mongoTemplate.find(query, RagChunk.class);

        return new PageImpl<>(content, pageable, total);
    }
}
