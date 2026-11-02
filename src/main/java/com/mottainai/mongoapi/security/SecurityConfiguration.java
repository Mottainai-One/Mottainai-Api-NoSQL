package com.mottainai.mongoapi.security;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableAspectJAutoProxy
public class SecurityConfiguration {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults())).build();
    }
    @Bean JwtDecoder jwtDecoder(@Value("${NOSQL_JWT_SECRET_BASE64:}") String secret,
                               @Value("${NOSQL_JWT_ISSUER:}") String issuer) {
        if (issuer.isBlank()) throw new IllegalStateException("NOSQL_JWT_ISSUER is required");
        byte[] key;
        try { key=Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("Invalid JWT key", e); }
        if (key.length < 32) throw new IllegalStateException("JWT key must be at least 256 bits");
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key,"HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> required = jwt -> jwt.getExpiresAt()!=null && jwt.getIssuedAt()!=null
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), required));
        return decoder;
    }
}
