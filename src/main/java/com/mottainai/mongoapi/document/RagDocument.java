package com.mottainai.mongoapi.document;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "rag_documents")
@CompoundIndexes({
        @CompoundIndex(name = "empresaId_title_version_idx", def = "{'empresaId': 1, 'title': 1, 'version': 1}", unique = true),
        @CompoundIndex(name = "empresaId_category_createdAt_idx", def = "{'empresaId': 1, 'category': 1, 'createdAt': -1}")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagDocument {

    @Id
    private String id;

    /**
     * Herdado do schema anterior: a IA Layer parou de preencher slug (e removeu seu índice único),
     * então documentos novos podem não ter esse campo. Mantido para os documentos que já o possuem.
     */
    @Indexed
    private String slug;

    @Indexed
    private Long empresaId;

    private String title;

    /** Herdado do schema anterior; hoje a IA Layer classifica os documentos por {@link #category}. */
    private String source;

    /** Classificação atual do documento na IA Layer (ex.: "FAQ", "MANUAL"). */
    private String category;

    /** Parte da chave única atual do documento: (empresaId, title, version). */
    private String version;

    private Map<String, Object> metadata;

    private Instant createdAt;
}
