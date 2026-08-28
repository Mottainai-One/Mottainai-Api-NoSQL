package com.mottainai.mongoapi.dto.rag;

import java.time.Instant;

public record RagDocumentResponse(
        String id,
        String slug,
        Long empresaId,
        String title,
        String source,
        Instant createdAt
) {
}
