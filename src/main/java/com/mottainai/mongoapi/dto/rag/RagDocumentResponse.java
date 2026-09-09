package com.mottainai.mongoapi.dto.rag;

import java.time.Instant;
import java.util.Map;

public record RagDocumentResponse(
        String id,
        String slug,
        Long empresaId,
        String title,
        String source,
        String category,
        String version,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
