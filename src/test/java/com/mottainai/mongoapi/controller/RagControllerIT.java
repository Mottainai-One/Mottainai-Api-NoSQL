package com.mottainai.mongoapi.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ValidationOptions;
import org.bson.BsonDocument;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottainai.mongoapi.AbstractIntegrationTest;
import com.mottainai.mongoapi.document.RagChunk;
import com.mottainai.mongoapi.document.RagDocument;

import org.springframework.data.mongodb.core.index.TextIndexDefinition;

class RagControllerIT extends AbstractIntegrationTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long OTHER_EMPRESA_ID = 2L;

    @Autowired
    private ObjectMapper objectMapper;

    private RagDocument seedDocument(Long empresaId, String slug) {
        return seedDocument(empresaId, slug, "Política de Estoque", "1.0");
    }

    private RagDocument seedDocument(Long empresaId, String slug, String title, String version) {
        RagDocument document = RagDocument.builder()
                .slug(slug).empresaId(empresaId).title(title).version(version)
                .category("MANUAL").source("manual_operacional").createdAt(Instant.now()).build();
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
                {"slug":"politica-estoque","empresaId":1,"title":"Política de Estoque",
                "source":"manual_operacional","category":"MANUAL","version":"1.0"}
                """;

        String response = mockMvc.perform(post("/api/v1/rag/documents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("politica-estoque"))
                .andExpect(jsonPath("$.version").value("1.0"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/rag/documents/{id}", id).param("empresaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("politica-estoque"))
                .andExpect(jsonPath("$.category").value("MANUAL"));
    }

    @Test
    void createDocument_respectsLiveMongoValidator() throws Exception {
        // O Mongo real exige empresaId como BSON int32 e category obrigatória.
        mongoTemplate.getDb().createCollection("rag_documents", new CreateCollectionOptions()
                .validationOptions(new ValidationOptions().validator(BsonDocument.parse("""
                        {"$jsonSchema":{"bsonType":"object",
                          "required":["empresaId","title","category","version","createdAt"],
                          "properties":{"empresaId":{"bsonType":"int"},
                            "category":{"enum":["MANUAL","FAQ","PROCEDIMENTO","TREINAMENTO","POLITICA"]}}}}
                        """))));

        String body = """
                {"slug":"faq-validador","empresaId":1,"title":"FAQ Validador","source":"faq_publica"}
                """;
        String response = mockMvc.perform(post("/api/v1/rag/documents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("FAQ"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();
        Document stored = mongoTemplate.getCollection("rag_documents")
                .find(new Document("_id", new org.bson.types.ObjectId(id))).first();
        org.assertj.core.api.Assertions.assertThat(stored).isNotNull();
        org.assertj.core.api.Assertions.assertThat(stored.get("empresaId")).isInstanceOf(Integer.class);
        mockMvc.perform(get("/api/v1/rag/documents/{id}", id).param("empresaId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void createDocument_withDuplicateSlug_returns409() throws Exception {
        seedDocument(EMPRESA_ID, "slug-existente", "Título A", "1.0");

        String body = """
                {"slug":"slug-existente","empresaId":1,"title":"Outro título","source":"faq","category":"FAQ","version":"1.0"}
                """;

        mockMvc.perform(post("/api/v1/rag/documents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createDocument_withoutSlug_succeedsAndReturnsCategoryAndVersion() throws Exception {
        // Documento legado sem slug/source, com category e metadata.
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
                {"chunk":0,"text":"texto do chunk","embedding":[0.1,0.2,0.3],"metadata":{"section":"manual"}}
                """;

        String response = mockMvc.perform(post("/api/v1/rag/documents/{id}/chunks", document.getId())
                        .param("empresaId", String.valueOf(EMPRESA_ID))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.embedding").doesNotExist())
                .andExpect(jsonPath("$.metadata.section").value("manual"))
                .andReturn().getResponse().getContentAsString();

        String chunkId = objectMapper.readTree(response).get("id").asText();
        RagChunk saved = mongoTemplate.findById(chunkId, RagChunk.class);
        org.assertj.core.api.Assertions.assertThat(saved).isNotNull();
        org.assertj.core.api.Assertions.assertThat(saved.getEmbedding()).isNull();
    }

    @Test
    void updateChunk_invalidatesStoredVectorOnlyWhenTextChanges() throws Exception {
        RagDocument document = seedDocument(EMPRESA_ID, "slug-vector-update");
        RagChunk chunk = mongoTemplate.save(RagChunk.builder().documentId(document.getId()).chunk(0)
                .text("original").embedding(java.util.List.of(0.1, 0.2)).createdAt(Instant.now()).build());

        mockMvc.perform(put("/api/v1/rag/chunks/{id}", chunk.getId()).param("empresaId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"original","metadata":{"section":"FAQ"}}
                                """))
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(mongoTemplate.findById(chunk.getId(), RagChunk.class).getEmbedding())
                .containsExactly(0.1, 0.2);

        mockMvc.perform(put("/api/v1/rag/chunks/{id}", chunk.getId()).param("empresaId", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"text":"novo conteúdo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embedding").doesNotExist());
        RagChunk stored = mongoTemplate.findById(chunk.getId(), RagChunk.class);
        org.assertj.core.api.Assertions.assertThat(stored.getText()).isEqualTo("novo conteúdo");
        org.assertj.core.api.Assertions.assertThat(stored.getEmbedding()).isNull();
        org.assertj.core.api.Assertions.assertThat(mongoTemplate.getCollection("rag_chunks")
                .countDocuments(new Document("documentId", new org.bson.types.ObjectId(document.getId()))
                        .append("embedding", new Document("$ne", null)))).isZero();
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
        // rag_documents.title só tem índice de texto em produção (criado pelo mongo-seed.js);
        // a API nunca cria índices, então o teste precisa recriá-lo aqui após o drop do @BeforeEach.
        mongoTemplate.indexOps(RagDocument.class)
                .ensureIndex(TextIndexDefinition.builder().onField("title").build());

        RagDocument ownDocument = seedDocument(EMPRESA_ID, "slug-busca-propria", "Controle de Perecíveis", "1.0");
        seedDocument(OTHER_EMPRESA_ID, "slug-busca-outra-empresa", "Controle de Perecíveis", "1.0");

        mockMvc.perform(get("/api/v1/rag/search")
                        .param("empresaId", String.valueOf(EMPRESA_ID))
                        .param("q", "Perecíveis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(ownDocument.getId()));
    }
}
