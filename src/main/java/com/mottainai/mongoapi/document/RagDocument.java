package com.mottainai.mongoapi.document;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "rag_documents")
@CompoundIndexes({
        @CompoundIndex(name = "ix_rag_documents_empresa_category_createdAt", def = "{'empresaId': 1, 'category': 1, 'createdAt': -1}"),
        /** Chave única atual da coleção: (empresaId, title, version). O slug deixou de ser único. */
        @CompoundIndex(name = "ux_rag_documents_empresa_title_version", def = "{'empresaId': 1, 'title': 1, 'version': 1}", unique = true),
        /** Índice de texto usado pelo endpoint /rag/search (vive em title, não mais em rag_chunks.text). */
        @CompoundIndex(name = "tx_rag_documents_title", def = "{'title': 'text'}")
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
    private String slug;

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
