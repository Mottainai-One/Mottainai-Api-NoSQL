package com.mottainai.mongoapi.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.mottainai.mongoapi.AbstractIntegrationTest;
import com.mottainai.mongoapi.document.Conversation;
import com.mottainai.mongoapi.document.Message;

class ConversationControllerIT extends AbstractIntegrationTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long OTHER_EMPRESA_ID = 2L;

    @Test
    void list_withoutEmpresaId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_fromAnotherEmpresa_returns404() throws Exception {
        mongoTemplate.save(Conversation.builder()
                .sessionId("session-1").empresaId(EMPRESA_ID).usuarioId(10L)
                .status("active").startedAt(Instant.now()).lastInteraction(Instant.now()).build());

        mockMvc.perform(get("/api/v1/conversations/{sessionId}", "session-1")
                        .param("empresaId", String.valueOf(OTHER_EMPRESA_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_ownedByEmpresa_returnsConversation() throws Exception {
        mongoTemplate.save(Conversation.builder()
                .sessionId("session-2").empresaId(EMPRESA_ID).usuarioId(10L)
                .status("active").startedAt(Instant.now()).lastInteraction(Instant.now()).build());

        mockMvc.perform(get("/api/v1/conversations/{sessionId}", "session-2")
                        .param("empresaId", String.valueOf(EMPRESA_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-2"));
    }

    @Test
    void listMessages_preservesRequestIdAndRagSourcesFromIa() throws Exception {
        Conversation conversation = mongoTemplate.save(Conversation.builder()
                .sessionId("session-trace").empresaId(EMPRESA_ID).usuarioId(10L)
                .agent("cliente").status("active").startedAt(Instant.now()).build());
        mongoTemplate.getCollection("messages").insertOne(new org.bson.Document()
                .append("empresaId", 1).append("conversationId", new org.bson.types.ObjectId(conversation.getId()))
                .append("requestId", "request-message").append("role", "assistant").append("content", "Resposta")
                .append("sources", java.util.List.of(new org.bson.Document("type", "rag")
                        .append("ref", "doc:123 chunk:0").append("score", 0.91)))
                .append("createdAt", java.util.Date.from(Instant.now())));

        mockMvc.perform(get("/api/v1/conversations/session-trace/messages").param("empresaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestId").value("request-message"))
                .andExpect(jsonPath("$.content[0].sources[0].ref").value("doc:123 chunk:0"))
                .andExpect(jsonPath("$.content[0].sources[0].score").value(0.91));
    }

    @Test
    void listMessages_forConversationOfAnotherEmpresa_returns404() throws Exception {
        Conversation conversation = mongoTemplate.save(Conversation.builder()
                .sessionId("session-3").empresaId(EMPRESA_ID).usuarioId(10L)
                .status("active").startedAt(Instant.now()).lastInteraction(Instant.now()).build());
        mongoTemplate.save(Message.builder().empresaId(EMPRESA_ID).conversationId(conversation.getId())
                .role("user").content("Olá").createdAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/conversations/{sessionId}/messages", "session-3")
                        .param("empresaId", String.valueOf(OTHER_EMPRESA_ID)))
                .andExpect(status().isNotFound());
    }
}
