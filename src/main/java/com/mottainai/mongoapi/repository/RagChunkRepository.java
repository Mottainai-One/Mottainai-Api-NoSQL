package com.mottainai.mongoapi.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.mottainai.mongoapi.document.RagChunk;

public interface RagChunkRepository extends MongoRepository<RagChunk, String> {

    Page<RagChunk> findByDocumentIdOrderByChunkAsc(String documentId, Pageable pageable);

    Optional<RagChunk> findTopByDocumentIdOrderByChunkDesc(String documentId);

    void deleteByDocumentId(String documentId);
}
