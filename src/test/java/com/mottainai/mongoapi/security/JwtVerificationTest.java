package com.mottainai.mongoapi.security;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

class JwtVerificationTest {
    private final byte[] key = "01234567890123456789012345678901".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private String token(byte[] secret, String issuer, Instant expiry) {
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(secret,"HmacSHA256")));
        var claims=JwtClaimsSet.builder().issuer(issuer).subject("uid").issuedAt(Instant.now().minusSeconds(3600)).expiresAt(expiry).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
    @Test void checksSignatureIssuerAndExpiry() {
        JwtDecoder decoder=new SecurityConfiguration().jwtDecoder(Base64.getEncoder().encodeToString(key), "issuer");
        assertDoesNotThrow(() -> decoder.decode(token(key,"issuer",Instant.now().plusSeconds(120))));
        assertThrows(JwtException.class, () -> decoder.decode(token("11234567890123456789012345678901".getBytes(),"issuer",Instant.now().plusSeconds(120))));
        assertThrows(JwtException.class, () -> decoder.decode(token(key,"other",Instant.now().plusSeconds(120))));
        assertThrows(JwtException.class, () -> decoder.decode(token(key,"issuer",Instant.now().minusSeconds(120))));
    }
    @Test void missingOrWeakConfigurationFailsClosed() {
        assertThrows(IllegalStateException.class, () -> new SecurityConfiguration().jwtDecoder("", "issuer"));
        assertThrows(IllegalStateException.class, () -> new SecurityConfiguration().jwtDecoder(Base64.getEncoder().encodeToString(key), ""));
    }
}
