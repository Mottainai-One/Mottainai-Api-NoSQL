package com.mottainai.mongoapi.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

class TenantAuthorizationTest {
    private static final java.util.UUID SESSION = java.util.UUID.fromString("12345678-1234-1234-1234-123456789abc");
    private final OperationalMembership membership = mock(OperationalMembership.class);
    private final TenantAuthorization authorization = new TenantAuthorization(membership);
    private Jwt token() {
        return Jwt.withTokenValue("token").header("alg", "HS256")
            .subject("firebase-uid").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
            .claim("sid", SESSION.toString()).claim("usuario_id", 4).claim("empresa_id", 7).claim("role", "DONO").claim("use", "access").build();
    }
    @Test void allowsActiveOwnerOfRequestedCompany() {
        when(membership.isActiveOwner("firebase-uid", 4L, 7L, SESSION)).thenReturn(true);
        assertDoesNotThrow(() -> authorization.authorize(token(), 7L));
    }
    @Test void rejectsDifferentCompanyWithoutQueryingMembership() {
        assertThrows(AccessDeniedException.class, () -> authorization.authorize(token(), 8L));
        verifyNoInteractions(membership);
    }
    @Test void rejectsRevokedMembership() {
        assertThrows(AccessDeniedException.class, () -> authorization.authorize(token(), 7L));
    }
    @Test void rejectsMissingSession() {
        Jwt noSession = Jwt.withTokenValue("x").header("alg", "HS256").subject("firebase-uid")
            .claim("usuario_id", 4).claim("empresa_id", 7).claim("role", "DONO").claim("use", "access").build();
        assertThrows(AccessDeniedException.class, () -> authorization.authorize(noSession, 7L));
        verifyNoInteractions(membership);
    }
    @Test void rejectsRefreshToken() {
        Jwt refresh = Jwt.withTokenValue("x").header("alg", "HS256").subject("firebase-uid")
            .claim("sid", SESSION.toString()).claim("usuario_id", 4).claim("empresa_id", 7).claim("role", "DONO").claim("use", "refresh").build();
        assertThrows(AccessDeniedException.class, () -> authorization.authorize(refresh, 7L));
    }
}
