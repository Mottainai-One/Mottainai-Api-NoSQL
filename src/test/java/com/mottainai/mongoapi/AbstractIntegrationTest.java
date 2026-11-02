package com.mottainai.mongoapi;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public abstract class AbstractIntegrationTest {
    // Repository/controller integration tests exercise data behavior. Authentication and
    // authorization are exercised with the real filters and aspect in SecurityRoutesTest.
    @org.springframework.boot.test.mock.mockito.MockBean
    private com.mottainai.mongoapi.security.TenantAuthorization authorization;
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder decoder;
    @org.junit.jupiter.api.AfterEach
    void clearSecurity() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }


    private static final String MONGO_IMAGE = "mongo:7.0.14";

    /**
     * Container "singleton": iniciado uma única vez e reaproveitado por todas as classes de IT
     * (padrão recomendado pelo Testcontainers). Evita reiniciar/repuxar a imagem a cada classe,
     * que é o que causava timeouts em série quando o pull da imagem estava lento.
     */
    private static final MongoDBContainer MONGO_CONTAINER = new MongoDBContainer(MONGO_IMAGE);

    static {
        MONGO_CONTAINER.start();
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri",
                () -> "mongodb://" + MONGO_CONTAINER.getHost() + ":" + MONGO_CONTAINER.getFirstMappedPort() + "/mottainai");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanDatabase() {
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("integration-test")
            .header("alg", "HS256").subject("test").build();
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .setAuthentication(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt));
        mongoTemplate.getDb().drop();
    }
}
