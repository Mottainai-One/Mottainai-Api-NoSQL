package com.mottainai.mongoapi.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.mottainai.mongoapi.document.RagChunk;
import com.mottainai.mongoapi.document.RagDocument;
import com.mottainai.mongoapi.dto.rag.RagChunkRequest;
import com.mottainai.mongoapi.dto.rag.RagChunkResponse;
import com.mottainai.mongoapi.dto.rag.RagDocumentRequest;
import com.mottainai.mongoapi.dto.rag.RagDocumentResponse;
import com.mottainai.mongoapi.exception.DuplicateDocumentException;
import com.mottainai.mongoapi.exception.ResourceNotFoundException;
import com.mottainai.mongoapi.mapper.RagMapper;
import com.mottainai.mongoapi.repository.RagChunkRepository;
import com.mottainai.mongoapi.repository.RagDocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RagService {

    private final RagDocumentRepository ragDocumentRepository;
    private final RagChunkRepository ragChunkRepository;
    private final RagMapper mapper;

    public Page<RagDocumentResponse> listDocuments(Long empresaId, String source, Pageable pageable) {
        Page<RagDocument> page = (source == null || source.isBlank())
                ? ragDocumentRepository.findByEmpresaId(empresaId, pageable)
                : ragDocumentRepository.findByEmpresaIdAndSource(empresaId, source, pageable);
        return page.map(mapper::toResponse);
    }

    public RagDocumentResponse getDocument(String id, Long empresaId) {
        return mapper.toResponse(findOwnedDocument(id, empresaId));
    }

    /** slug pode ter mais de uma versão; devolve a mais recente. */
    public RagDocumentResponse getDocumentBySlug(String slug, Long empresaId) {
        RagDocument document = ragDocumentRepository.findFirstBySlugAndEmpresaIdOrderByCreatedAtDesc(slug, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado: " + slug));
        return mapper.toResponse(document);
    }

    public RagDocumentResponse createDocument(RagDocumentRequest request) {
        ragDocumentRepository
                .findByEmpresaIdAndTitleAndVersion(request.empresaId(), request.title(), request.version())
                .ifPresent(existing -> {
                    throw new DuplicateDocumentException(
                            "Já existe um documento com este título e versão: " + request.title() + " v" + request.version());
                });

        RagDocument document = mapper.toEntity(request);
        document.setCreatedAt(Instant.now());
        RagDocument saved = ragDocumentRepository.save(document);
        return mapper.toResponse(saved);
    }

    public RagDocumentResponse updateDocument(String id, Long empresaId, RagDocumentRequest request) {
        RagDocument document = findOwnedDocument(id, empresaId);

        ragDocumentRepository.findByEmpresaIdAndTitleAndVersion(empresaId, request.title(), request.version())
                .filter(existing -> !existing.getId().equals(document.getId()))
                .ifPresent(existing -> {
                    throw new DuplicateDocumentException(
                            "Já existe um documento com este título e versão: " + request.title() + " v" + request.version());
                });

        mapper.updateEntity(request, document);
        RagDocument saved = ragDocumentRepository.save(document);
        return mapper.toResponse(saved);
    }

    public void deleteDocument(String id, Long empresaId) {
        RagDocument document = findOwnedDocument(id, empresaId);
        ragChunkRepository.deleteByDocumentId(document.getId());
        ragDocumentRepository.delete(document);
    }

    public Page<RagChunkResponse> listChunks(String documentId, Long empresaId, Pageable pageable) {
        findOwnedDocument(documentId, empresaId);
        return ragChunkRepository.findByDocumentIdOrderByChunkAsc(documentId, pageable).map(mapper::toResponse);
    }

    public RagChunkResponse createChunk(String documentId, Long empresaId, RagChunkRequest request) {
        findOwnedDocument(documentId, empresaId);

        int chunkIndex = request.chunk() != null
                ? request.chunk()
                : ragChunkRepository.findTopByDocumentIdOrderByChunkDesc(documentId)
                        .map(existing -> existing.getChunk() + 1)
                        .orElse(0);

        RagChunk chunk = RagChunk.builder()
                .documentId(documentId)
                .chunk(chunkIndex)
                .text(request.text())
                .embedding(null)
                .metadata(request.metadata())
                .createdAt(Instant.now())
                .build();

        RagChunk saved = ragChunkRepository.save(chunk);
        return mapper.toResponse(saved);
    }

    public RagChunkResponse updateChunk(String id, Long empresaId, RagChunkRequest request) {
        RagChunk chunk = findOwnedChunk(id, empresaId);
        chunk.setText(request.text());
        if (request.chunk() != null) {
            chunk.setChunk(request.chunk());
        }
        if (request.metadata() != null) {
            chunk.setMetadata(request.metadata());
        }
        RagChunk saved = ragChunkRepository.save(chunk);
        return mapper.toResponse(saved);
    }

    public void deleteChunk(String id, Long empresaId) {
        RagChunk chunk = findOwnedChunk(id, empresaId);
        ragChunkRepository.delete(chunk);
    }

    /** Busca textual em rag_documents.title (índice de texto), restrita aos documentos da empresa. */
    public Page<RagDocumentResponse> search(Long empresaId, String query, Pageable pageable) {
        return ragDocumentRepository.searchByTitleText(empresaId, query, pageable).map(mapper::toResponse);
    }

    private RagDocument findOwnedDocument(String id, Long empresaId) {
        return ragDocumentRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado: " + id));
    }

    private RagChunk findOwnedChunk(String id, Long empresaId) {
        RagChunk chunk = ragChunkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chunk não encontrado: " + id));
        findOwnedDocument(chunk.getDocumentId(), empresaId);
        return chunk;
    }
}
