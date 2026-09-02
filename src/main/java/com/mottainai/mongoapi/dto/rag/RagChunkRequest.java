package com.mottainai.mongoapi.dto.rag;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Sem campo "embedding" propositalmente: é calculado pela IA Layer e a API nunca aceita esse valor de entrada,
 * mesmo que venha no corpo da requisição (Jackson descarta propriedades desconhecidas por padrão).
 */
public record RagChunkRequest(
        @PositiveOrZero
        Integer chunk,

        @NotBlank
        @Size(max = 8000)
        String text,

        Map<String, Object> metadata
) {
}
