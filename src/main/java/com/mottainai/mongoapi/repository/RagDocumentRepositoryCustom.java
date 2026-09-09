package com.mottainai.mongoapi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mottainai.mongoapi.document.RagDocument;

public interface RagDocumentRepositoryCustom {

    /**
     * Busca textual em rag_documents.title (índice de texto tx_rag_documents_title), restrita à
     * empresa informada. O filtro de tenant é aplicado como critério da própria query, nunca depois.
     */
    Page<RagDocument> searchByTitleText(Long empresaId, String text, Pageable pageable);
}
