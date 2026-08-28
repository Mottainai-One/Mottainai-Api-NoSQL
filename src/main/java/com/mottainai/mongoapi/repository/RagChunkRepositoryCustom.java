package com.mottainai.mongoapi.repository;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mottainai.mongoapi.document.RagChunk;

public interface RagChunkRepositoryCustom {

    /**
     * Busca textual restrita a um conjunto de documentIds já resolvido a partir do empresaId
     * (rag_chunks não guarda empresaId — o filtro de tenant é aplicado ANTES da busca textual,
     * resolvendo os documentos da empresa primeiro, nunca depois).
     */
    Page<RagChunk> searchByTextInDocuments(String text, Collection<String> documentIds, Pageable pageable);
}
