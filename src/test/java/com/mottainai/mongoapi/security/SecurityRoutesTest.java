package com.mottainai.mongoapi.security;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import com.mottainai.mongoapi.controller.MemoryController;
import com.mottainai.mongoapi.controller.RagController;
import com.mottainai.mongoapi.service.MemoryService;
import com.mottainai.mongoapi.service.RagService;

@WebMvcTest({MemoryController.class, RagController.class})
@Import({SecurityConfiguration.class, TenantAuthorization.class, TenantAuthorizationAspect.class})
class SecurityRoutesTest {
    private static final java.util.UUID SESSION = java.util.UUID.fromString("12345678-1234-1234-1234-123456789abc");
    @Autowired MockMvc mvc;
    @MockBean JwtDecoder decoder;
    @MockBean OperationalMembership membership;
    @MockBean MemoryService memories;
    @MockBean RagService rag;
    @BeforeEach void setup() {
        Jwt jwt=Jwt.withTokenValue("valid").header("alg", "HS256").subject("uid")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
            .claim("sid", SESSION.toString()).claim("usuario_id", 4).claim("empresa_id", 7).claim("role", "DONO").claim("use", "access").build();
        when(decoder.decode("valid")).thenReturn(jwt);
        when(membership.isActiveOwner("uid", 4L, 7L, SESSION)).thenReturn(true);
    }
    @Test void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/memories/7/4")).andExpect(status().isUnauthorized());
        verifyNoInteractions(memories);
    }
    @Test void activeOwnerCanAccessOwnCompany() throws Exception {
        mvc.perform(get("/api/v1/memories/7/4").header("Authorization", "Bearer valid"))
            .andExpect(status().isOk());
        verify(memories).get(7L, 4L);
    }
    @Test void crossCompanyPathIsRejected() throws Exception {
        mvc.perform(delete("/api/v1/memories/8/4").header("Authorization", "Bearer valid"))
            .andExpect(status().isNotFound());
        verifyNoInteractions(memories);
    }
    @Test void crossCompanyQueryIsRejected() throws Exception {
        mvc.perform(get("/api/v1/rag/documents").param("empresaId", "8").header("Authorization", "Bearer valid"))
            .andExpect(status().isNotFound());
        verifyNoInteractions(rag);
    }
    @Test void crossCompanyBodyIsRejected() throws Exception {
        mvc.perform(post("/api/v1/rag/documents").header("Authorization", "Bearer valid")
            .contentType(MediaType.APPLICATION_JSON).content("{\"empresaId\":8,\"title\":\"x\"}"))
            .andExpect(status().isNotFound());
        verifyNoInteractions(rag);
    }
    @Test void inactiveOwnerIsRejected() throws Exception {
        when(membership.isActiveOwner("uid",4L,7L,SESSION)).thenReturn(false);
        mvc.perform(get("/api/v1/memories/7/4").header("Authorization", "Bearer valid"))
            .andExpect(status().isNotFound());
        verifyNoInteractions(memories);
    }
}
