package com.mottainai.mongoapi.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.mottainai.mongoapi.document.RagChunk;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RagChunkRepositoryImpl implements RagChunkRepositoryCustom {

    /** Caracteres com significado especial em regex, escapados para tratar o termo como literal. */
    private static final String REGEX_METACHARACTERS = "\\^$.|?*+()[]{}";

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<RagChunk> searchByTextInDocuments(String text, Collection<String> documentIds, Pageable pageable) {
        if (documentIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // A IA Layer removeu o índice de texto de rag_chunks.text (hoje o índice de texto vive em
        // rag_documents.title), então um $text aqui falha com IndexNotFound. A busca usa regex
        // case-insensitive sobre o subconjunto já restrito aos documentos da empresa — o filtro de
        // tenant continua sendo resolvido antes da busca, nunca depois.
        List<Criteria> termCriteria = Arrays.stream(text.trim().split("\\s+"))
                .filter(term -> !term.isBlank())
                .map(term -> Criteria.where("text").regex(escapeRegex(term), "i"))
                .toList();

        if (termCriteria.isEmpty()) {
            return Page.empty(pageable);
        }

        Criteria criteria = Criteria.where("documentId").in(documentIds)
                .orOperator(termCriteria.toArray(new Criteria[0]));

        Query query = new Query(criteria);
        long total = mongoTemplate.count(query, RagChunk.class);

        query.with(pageable);
        if (pageable.getSort().isUnsorted()) {
            // Sem score de $text não há ranking de relevância; ordena de forma estável e previsível.
            query.with(Sort.by(Sort.Order.asc("documentId"), Sort.Order.asc("chunk")));
        }

        List<RagChunk> content = mongoTemplate.find(query, RagChunk.class);

        return new PageImpl<>(content, pageable, total);
    }

    private static String escapeRegex(String term) {
        StringBuilder escaped = new StringBuilder(term.length() * 2);
        for (char c : term.toCharArray()) {
            if (REGEX_METACHARACTERS.indexOf(c) >= 0) {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
