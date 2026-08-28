package com.mottainai.mongoapi.dto.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RagDocumentRequest(
        @NotBlank
        @Size(max = 150)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "deve conter apenas letras minúsculas, números e hífens")
        String slug,

        @NotNull
        Long empresaId,

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 100)
        String source
) {
}
