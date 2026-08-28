package com.mottainai.mongoapi.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "rag_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String slug;

    @Indexed
    private Long empresaId;

    private String title;
    private String source;
    private Instant createdAt;
}
