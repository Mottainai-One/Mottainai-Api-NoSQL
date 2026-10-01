package com.mottainai.mongoapi.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.mottainai.mongoapi.document.RagDocument;

public interface RagDocumentRepository extends MongoRepository<RagDocument, String>, RagDocumentRepositoryCustom {

    Page<RagDocument> findByEmpresaId(Long empresaId, Pageable pageable);

    Page<RagDocument> findByEmpresaIdAndSource(Long empresaId, String source, Pageable pageable);

    Page<RagDocument> findByEmpresaIdAndCategory(Long empresaId, String category, Pageable pageable);

    Optional<RagDocument> findByIdAndEmpresaId(String id, Long empresaId);

    /** Índice único (empresaId, slug); a ordenação mantém compatibilidade com chamadas existentes. */
    Optional<RagDocument> findFirstBySlugAndEmpresaIdOrderByCreatedAtDesc(String slug, Long empresaId);

    /** Usada para a checagem de conflito 409: a chave única atual é (empresaId, title, version). */
    Optional<RagDocument> findByEmpresaIdAndTitleAndVersion(Long empresaId, String title, String version);
}
