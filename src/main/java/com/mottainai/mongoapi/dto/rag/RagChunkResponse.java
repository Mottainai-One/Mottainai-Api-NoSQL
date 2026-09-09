package com.mottainai.mongoapi.dto.rag;

import java.time.Instant;
import java.util.Map;

public record RagChunkResponse(
        String id,
        String documentId,
        Integer chunk,
        String text,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
