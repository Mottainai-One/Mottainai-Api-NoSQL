package com.mottainai.mongoapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.mottainai.mongoapi.document.RagDocument;

public interface RagDocumentRepository extends MongoRepository<RagDocument, String> {

    Page<RagDocument> findByEmpresaId(Long empresaId, Pageable pageable);

    Page<RagDocument> findByEmpresaIdAndSource(Long empresaId, String source, Pageable pageable);

    Page<RagDocument> findByEmpresaIdAndCategory(Long empresaId, String category, Pageable pageable);

    /** Chave única atual da coleção (índice ux_rag_documents_empresa_title_version). */
    Optional<RagDocument> findByEmpresaIdAndTitleAndVersion(Long empresaId, String title, String version);

    /** Usada apenas para resolver os IDs de documentos da empresa antes da busca textual em rag_chunks. */
    List<RagDocument> findByEmpresaId(Long empresaId);

    Optional<RagDocument> findByIdAndEmpresaId(String id, Long empresaId);

    Optional<RagDocument> findBySlugAndEmpresaId(String slug, Long empresaId);

    /** slug é globalmente único (não por empresa) — usado para a checagem de conflito 409. */
    Optional<RagDocument> findBySlug(String slug);
}
