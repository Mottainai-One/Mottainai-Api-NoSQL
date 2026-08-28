package com.mottainai.mongoapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mongoApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mottainai — Mongo API")
                        .description("Camada REST de consulta e curadoria do MongoDB da IA Layer do ecossistema Mottainai. "
                                + "conversations/messages/metrics/agent_executions são somente leitura; "
                                + "rag_documents/rag_chunks têm CRUD completo; memories permite leitura e exclusão.")
                        .version("v1")
                        .contact(new Contact().name("Mottainai")));
    }
}
