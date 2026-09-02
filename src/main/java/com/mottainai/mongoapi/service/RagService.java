package com.mottainai.mongoapi.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.mottainai.mongoapi.document.RagChunk;
import com.mottainai.mongoapi.document.RagDocument;
import com.mottainai.mongoapi.dto.rag.RagChunkRequest;
import com.mottainai.mongoapi.dto.rag.RagChunkResponse;
import com.mottainai.mongoapi.dto.rag.RagDocumentRequest;
import com.mottainai.mongoapi.dto.rag.RagDocumentResponse;
import com.mottainai.mongoapi.exception.DuplicateSlugException;
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

    public Page<RagDocumentResponse> listDocuments(Long empresaId, String source, String category, Pageable pageable) {
        Page<RagDocument> page;
        if (category != null && !category.isBlank()) {
            page = ragDocumentRepository.findByEmpresaIdAndCategory(empresaId, category, pageable);
        } else if (source != null && !source.isBlank()) {
            page = ragDocumentRepository.findByEmpresaIdAndSource(empresaId, source, pageable);
        } else {
            page = ragDocumentRepository.findByEmpresaId(empresaId, pageable);
        }
        return page.map(mapper::toResponse);
    }

    public RagDocumentResponse getDocument(String id, Long empresaId) {
        return mapper.toResponse(findOwnedDocument(id, empresaId));
    }

    public RagDocumentResponse getDocumentBySlug(String slug, Long empresaId) {
        RagDocument document = ragDocumentRepository.findBySlugAndEmpresaId(slug, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado: " + slug));
        return mapper.toResponse(document);
    }

    public RagDocumentResponse createDocument(RagDocumentRequest request) {
        checkSlugAvailable(request, null);
        checkTitleVersionAvailable(request, null);

        RagDocument document = mapper.toEntity(request);
        document.setVersion(request.versionOrDefault());
        document.setCreatedAt(Instant.now());
        RagDocument saved = ragDocumentRepository.save(document);
        return mapper.toResponse(saved);
    }

    public RagDocumentResponse updateDocument(String id, Long empresaId, RagDocumentRequest request) {
        RagDocument document = findOwnedDocument(id, empresaId);

        checkSlugAvailable(request, document.getId());
        checkTitleVersionAvailable(request, document.getId());

        mapper.updateEntity(request, document);
        document.setVersion(request.versionOrDefault());
        RagDocument saved = ragDocumentRepository.save(document);
        return mapper.toResponse(saved);
    }

    /** O slug deixou de ser obrigatório (a IA Layer não o preenche mais), mas continua único quando informado. */
    private void checkSlugAvailable(RagDocumentRequest request, String currentDocumentId) {
        if (request.slug() == null || request.slug().isBlank()) {
            return;
        }
        ragDocumentRepository.findBySlug(request.slug())
                .filter(existing -> !existing.getId().equals(currentDocumentId))
                .ifPresent(existing -> {
                    throw new DuplicateSlugException("Já existe um documento com o slug: " + request.slug());
                });
    }

    /** Reflete o índice único atual da coleção: (empresaId, title, version). */
    private void checkTitleVersionAvailable(RagDocumentRequest request, String currentDocumentId) {
        String version = request.versionOrDefault();
        ragDocumentRepository.findByEmpresaIdAndTitleAndVersion(request.empresaId(), request.title(), version)
                .filter(existing -> !existing.getId().equals(currentDocumentId))
                .ifPresent(existing -> {
                    throw new DuplicateSlugException(
                            "Já existe um documento com o título \"" + request.title() + "\" na versão " + version);
                });
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

    public Page<RagChunkResponse> search(Long empresaId, String query, Pageable pageable) {
        List<String> documentIds = ragDocumentRepository.findByEmpresaId(empresaId).stream()
                .map(RagDocument::getId)
                .toList();
        return ragChunkRepository.searchByTextInDocuments(query, documentIds, pageable).map(mapper::toResponse);
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
