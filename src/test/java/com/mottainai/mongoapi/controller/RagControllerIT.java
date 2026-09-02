package com.mottainai.mongoapi.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottainai.mongoapi.AbstractIntegrationTest;
import com.mottainai.mongoapi.document.RagChunk;
import com.mottainai.mongoapi.document.RagDocument;

class RagControllerIT extends AbstractIntegrationTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long OTHER_EMPRESA_ID = 2L;

    @Autowired
    private ObjectMapper objectMapper;

    private RagDocument seedDocument(Long empresaId, String slug) {
        RagDocument document = RagDocument.builder()
                .slug(slug).empresaId(empresaId).title("Política de Estoque")
                .source("manual_operacional").createdAt(Instant.now()).build();
        return mongoTemplate.save(document);
    }

    @Test
    void listDocuments_withoutEmpresaId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/rag/documents"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createDocument_thenGetById_success() throws Exception {
        String body = """
                {"slug":"politica-estoque","empresaId":1,"title":"Política de Estoque","source":"manual_operacional"}
                """;

        String response = mockMvc.perform(post("/api/v1/rag/documents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("politica-estoque"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/rag/documents/{id}", id).param("empresaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("politica-estoque"));
    }

    @Test
    void createDocument_withDuplicateSlug_returns409() throws Exception {
        seedDocument(EMPRESA_ID, "slug-existente");

        String body = """
                {"slug":"slug-existente","empresaId":1,"title":"Outro título","source":"faq"}
                """;

        mockMvc.perform(post("/api/v1/rag/documents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createDocument_withoutSlug_succeedsAndReturnsCategoryAndVersion() throws Exception {
        // Documento no formato que a IA Layer grava hoje: sem slug/source, com category e metadata.
        String body = """
                {"empresaId":1,"title":"FAQ Promoções Mottainai","category":"FAQ","metadata":{"audience":"cliente"}}
                """;

        mockMvc.perform(post("/api/v1/rag/documents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").doesNotExist())
                .andExpect(jsonPath("$.category").value("FAQ"))
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.metadata.audience").value("cliente"));
    }

    @Test
    void createDocument_withSameTitleAndVersionForSameEmpresa_returns409() throws Exception {
        mongoTemplate.save(RagDocument.builder().empresaId(EMPRESA_ID).title("Manual de Estoque")
                .category("MANUAL").version("1.0").createdAt(Instant.now()).build());

        String body = """
                {"empresaId":1,"title":"Manual de Estoque","category":"MANUAL","version":"1.0"}
                """;

        mockMvc.perform(post("/api/v1/rag/documents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void listDocuments_filteredByCategory_returnsOnlyThatCategory() throws Exception {
        mongoTemplate.save(RagDocument.builder().empresaId(EMPRESA_ID).title("FAQ A")
                .category("FAQ").version("1.0").createdAt(Instant.now()).build());
        mongoTemplate.save(RagDocument.builder().empresaId(EMPRESA_ID).title("Manual A")
                .category("MANUAL").version("1.0").createdAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/rag/documents")
                        .param("empresaId", String.valueOf(EMPRESA_ID))
                        .param("category", "MANUAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Manual A"));
    }

    @Test
    void getDocument_fromAnotherEmpresa_returns404() throws Exception {
        RagDocument document = seedDocument(EMPRESA_ID, "slug-empresa-1");

        mockMvc.perform(get("/api/v1/rag/documents/{id}", document.getId()).param("empresaId", String.valueOf(OTHER_EMPRESA_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDocument_cascadesToChunks() throws Exception {
        RagDocument document = seedDocument(EMPRESA_ID, "slug-cascata");
        mongoTemplate.save(RagChunk.builder().documentId(document.getId()).chunk(0).text("chunk 0").createdAt(Instant.now()).build());
        mongoTemplate.save(RagChunk.builder().documentId(document.getId()).chunk(1).text("chunk 1").createdAt(Instant.now()).build());

        mockMvc.perform(delete("/api/v1/rag/documents/{id}", document.getId()).param("empresaId", String.valueOf(EMPRESA_ID)))
                .andExpect(status().isNoContent());

        long remaining = mongoTemplate.count(
                org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("documentId").is(document.getId())),
                RagChunk.class);
        org.assertj.core.api.Assertions.assertThat(remaining).isZero();
    }

    @Test
    void createChunk_withoutChunkNumber_autoIncrementsFromMax() throws Exception {
        RagDocument document = seedDocument(EMPRESA_ID, "slug-chunks");
        mongoTemplate.save(RagChunk.builder().documentId(document.getId()).chunk(3).text("existente").createdAt(Instant.now()).build());

        String body = """
                {"text":"novo chunk sem número explícito"}
                """;

        mockMvc.perform(post("/api/v1/rag/documents/{id}/chunks", document.getId())
                        .param("empresaId", String.valueOf(EMPRESA_ID))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chunk").value(4));
    }

    @Test
    void createChunk_withEmbeddingInPayload_isIgnoredAndNeverReturned() throws Exception {
        RagDocument document = seedDocument(EMPRESA_ID, "slug-embedding");

        String body = """
                {"chunk":0,"text":"texto do chunk","embedding":[0.1,0.2,0.3]}
                """;

        String response = mockMvc.perform(post("/api/v1/rag/documents/{id}/chunks", document.getId())
                        .param("empresaId", String.valueOf(EMPRESA_ID))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.embedding").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String chunkId = objectMapper.readTree(response).get("id").asText();
        RagChunk saved = mongoTemplate.findById(chunkId, RagChunk.class);
        org.assertj.core.api.Assertions.assertThat(saved).isNotNull();
        org.assertj.core.api.Assertions.assertThat(saved.getEmbedding()).isNull();
    }

    @Test
    void updateChunk_fromAnotherEmpresa_returns404() throws Exception {
        RagDocument document = seedDocument(EMPRESA_ID, "slug-update-chunk");
        RagChunk chunk = mongoTemplate.save(RagChunk.builder().documentId(document.getId()).chunk(0).text("original").createdAt(Instant.now()).build());

        String body = """
                {"text":"tentativa de outra empresa"}
                """;

        mockMvc.perform(put("/api/v1/rag/chunks/{id}", chunk.getId())
                        .param("empresaId", String.valueOf(OTHER_EMPRESA_ID))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_isRestrictedToOwnEmpresaDocuments() throws Exception {
        // Sem criar índice de texto de propósito: a IA Layer removeu o índice de texto de
        // rag_chunks.text, então a busca precisa funcionar exatamente como em produção — sem ele.
        RagDocument ownDocument = seedDocument(EMPRESA_ID, "slug-busca-propria");
        mongoTemplate.save(RagChunk.builder().documentId(ownDocument.getId()).chunk(0)
                .text("controle de estoque perecível").createdAt(Instant.now()).build());

        RagDocument otherDocument = seedDocument(OTHER_EMPRESA_ID, "slug-busca-outra-empresa");
        mongoTemplate.save(RagChunk.builder().documentId(otherDocument.getId()).chunk(0)
                .text("controle de estoque perecível").createdAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/rag/search")
                        .param("empresaId", String.valueOf(EMPRESA_ID))
                        .param("q", "perecível"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].documentId").value(ownDocument.getId()));
    }

    @Test
    void search_withRegexMetacharactersInQuery_treatsThemAsLiteralText() throws Exception {
        RagDocument document = seedDocument(EMPRESA_ID, "slug-busca-metacaractere");
        mongoTemplate.save(RagChunk.builder().documentId(document.getId()).chunk(0)
                .text("desconto de 20% (vinte por cento) em perecíveis").createdAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/rag/search")
                        .param("empresaId", String.valueOf(EMPRESA_ID))
                        .param("q", "(vinte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }
}
