package com.mottainai.mongoapi.dto.rag;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RagDocumentRequest(
        /** Opcional para manter compatibilidade; o Mongo permite um único slug ausente por empresa. */
        @Size(max = 150)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "deve conter apenas letras minúsculas, números e hífens")
        String slug,

        @NotNull
        @Positive
        @Max(Integer.MAX_VALUE)
        Long empresaId,

        @NotBlank
        @Size(max = 200)
        String title,

        /** Herdado do schema anterior; hoje a classificação usada pela IA Layer é category. */
        @Size(max = 100)
        String source,

        /** Quando omitida, é derivada de source como na IA Layer. */
        @Pattern(regexp = "MANUAL|FAQ|PROCEDIMENTO|TREINAMENTO|POLITICA",
                message = "deve ser MANUAL, FAQ, PROCEDIMENTO, TREINAMENTO ou POLITICA")
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

    public String categoryOrDefault() {
        if (category != null) {
            return category;
        }
        if (source == null) {
            return "MANUAL";
        }
        String normalized = source.strip().toUpperCase(java.util.Locale.ROOT);
        if (normalized.startsWith("FAQ")) {
            return "FAQ";
        }
        if (normalized.startsWith("PROCEDIMENTO")) {
            return "PROCEDIMENTO";
        }
        if (normalized.startsWith("TREINAMENTO")) {
            return "TREINAMENTO";
        }
        if (normalized.startsWith("POLITICA")) {
            return "POLITICA";
        }
        return "MANUAL";
    }
}
