package com.mottainai.mongoapi.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mottainai.mongoapi.dto.rag.RagChunkRequest;
import com.mottainai.mongoapi.dto.rag.RagChunkResponse;
import com.mottainai.mongoapi.dto.rag.RagDocumentRequest;
import com.mottainai.mongoapi.dto.rag.RagDocumentResponse;
import com.mottainai.mongoapi.service.RagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Tag(name = "RAG", description = "CRUD completo — base de conhecimento curada (rag_documents / rag_chunks)")
public class RagController {

    private final RagService ragService;

    @GetMapping("/documents")
    @Operation(summary = "Lista documentos RAG de uma empresa, com filtro opcional por categoria "
            + "(classificação atual da IA Layer) ou por fonte (campo herdado do schema anterior). "
            + "Quando ambos são informados, category tem precedência.")
    public Page<RagDocumentResponse> listDocuments(
            @RequestParam Long empresaId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String category,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return ragService.listDocuments(empresaId, source, category, pageable);
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "Busca um documento RAG pelo id, restrito à empresa informada")
    public RagDocumentResponse getDocument(@PathVariable String id, @RequestParam Long empresaId) {
        return ragService.getDocument(id, empresaId);
    }

    @GetMapping("/documents/slug/{slug}")
    @Operation(summary = "Busca um documento RAG pelo slug, restrito à empresa informada")
    public RagDocumentResponse getDocumentBySlug(@PathVariable String slug, @RequestParam Long empresaId) {
        return ragService.getDocumentBySlug(slug, empresaId);
    }

    @PostMapping("/documents")
    @Operation(summary = "Cria um documento RAG (409 se já existir um documento com o mesmo título e versão)")
    public ResponseEntity<RagDocumentResponse> createDocument(@Valid @RequestBody RagDocumentRequest request) {
        RagDocumentResponse created = ragService.createDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/documents/{id}")
    @Operation(summary = "Atualiza título/fonte/slug/categoria/versão/metadata de um documento RAG. "
            + "O empresaId do corpo é ignorado; o dono do documento nunca muda após a criação.")
    public RagDocumentResponse updateDocument(
            @PathVariable String id,
            @RequestParam Long empresaId,
            @Valid @RequestBody RagDocumentRequest request) {
        return ragService.updateDocument(id, empresaId, request);
    }

    @DeleteMapping("/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um documento RAG e, em cascata, todos os seus chunks")
    public void deleteDocument(@PathVariable String id, @RequestParam Long empresaId) {
        ragService.deleteDocument(id, empresaId);
    }

    @GetMapping("/documents/{id}/chunks")
    @Operation(summary = "Lista os chunks de um documento, ordenados por número do chunk")
    public Page<RagChunkResponse> listChunks(
            @PathVariable String id,
            @RequestParam Long empresaId,
            @PageableDefault(sort = "chunk") Pageable pageable) {
        return ragService.listChunks(id, empresaId, pageable);
    }

    @PostMapping("/documents/{id}/chunks")
    @Operation(summary = "Cria um chunk. Se 'chunk' não vier, usa max(chunk)+1 do documento. "
            + "'embedding' nunca é aceito (é calculado pela IA Layer).")
    public ResponseEntity<RagChunkResponse> createChunk(
            @PathVariable String id,
            @RequestParam Long empresaId,
            @Valid @RequestBody RagChunkRequest request) {
        RagChunkResponse created = ragService.createChunk(id, empresaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/chunks/{id}")
    @Operation(summary = "Atualiza o texto (e, opcionalmente, o número) de um chunk")
    public RagChunkResponse updateChunk(
            @PathVariable String id,
            @RequestParam Long empresaId,
            @Valid @RequestBody RagChunkRequest request) {
        return ragService.updateChunk(id, empresaId, request);
    }

    @DeleteMapping("/chunks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um chunk")
    public void deleteChunk(@PathVariable String id, @RequestParam Long empresaId) {
        ragService.deleteChunk(id, empresaId);
    }

    @GetMapping("/search")
    @Operation(summary = "Busca textual em rag_documents.title, restrita à empresa informada "
            + "(o filtro de tenant é aplicado na própria query, nunca depois)")
    public Page<RagDocumentResponse> search(
            @RequestParam Long empresaId,
            @RequestParam String q,
            @PageableDefault Pageable pageable) {
        return ragService.search(empresaId, q, pageable);
    }
}
