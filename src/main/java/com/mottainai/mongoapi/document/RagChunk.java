package com.mottainai.mongoapi.document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "rag_chunks")
@CompoundIndexes({
        @CompoundIndex(name = "documentId_chunk_idx", def = "{'documentId': 1, 'chunk': 1}")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagChunk {

    @Id
    private String id;

    @Indexed
    @Field(targetType = FieldType.OBJECT_ID)
    private String documentId;

    private Integer chunk;
    private String text;

    /** Calculada exclusivamente pela IA Layer; a API nunca aceita nem devolve este campo. */
    private List<Double> embedding;

    private Map<String, Object> metadata;

    private Instant createdAt;
}
