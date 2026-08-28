package com.mottainai.mongoapi.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mottainai.mongoapi.AbstractIntegrationTest;
import com.mottainai.mongoapi.document.Memory;

class MemoryControllerIT extends AbstractIntegrationTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long OTHER_EMPRESA_ID = 2L;

    @Test
    void list_withoutEmpresaId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/memories"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_forAnotherEmpresa_returns404() throws Exception {
        mongoTemplate.save(Memory.builder().empresaId(EMPRESA_ID).usuarioId(10L)
                .preferences(List.of()).facts(List.of()).updatedAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/memories/{empresaId}/{usuarioId}", OTHER_EMPRESA_ID, 10L))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_removesMemory_thenGetReturns404() throws Exception {
        mongoTemplate.save(Memory.builder().empresaId(EMPRESA_ID).usuarioId(20L)
                .preferences(List.of("compacto")).facts(List.of()).updatedAt(Instant.now()).build());

        mockMvc.perform(delete("/api/v1/memories/{empresaId}/{usuarioId}", EMPRESA_ID, 20L))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/memories/{empresaId}/{usuarioId}", EMPRESA_ID, 20L))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_returnsPagedMemoriesOfEmpresa() throws Exception {
        mongoTemplate.save(Memory.builder().empresaId(EMPRESA_ID).usuarioId(30L)
                .preferences(List.of()).facts(List.of()).updatedAt(Instant.now()).build());

        mockMvc.perform(get("/api/v1/memories").param("empresaId", String.valueOf(EMPRESA_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
