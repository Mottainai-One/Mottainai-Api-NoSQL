package com.mottainai.mongoapi.dto.rag;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RagDocumentRequest(
        /**
         * Opcional: a IA Layer parou de preencher slug e removeu seu índice único, então documentos
         * novos podem não ter um. Quando informado, continua sendo validado e tratado como único.
         */
        @Size(max = 150)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "deve conter apenas letras minúsculas, números e hífens")
        String slug,

        @NotNull
        Long empresaId,

        @NotBlank
        @Size(max = 200)
        String title,

        /** Herdado do schema anterior; hoje a classificação usada pela IA Layer é category. */
        @Size(max = 100)
        String source,

        /** Classificação atual do documento na IA Layer (ex.: "FAQ", "MANUAL"). */
        @Size(max = 100)
        String category,

        /** Compõe a chave única atual (empresaId, title, version). Assume "1.0" quando omitido. */
        @Size(max = 50)
        String version,

        Map<String, Object> metadata
) {

    /** Versão default aplicada quando o cliente não informa uma, alinhada ao que a IA Layer grava. */
    public static final String DEFAULT_VERSION = "1.0";

    public String versionOrDefault() {
        return (version == null || version.isBlank()) ? DEFAULT_VERSION : version;
    }
}
