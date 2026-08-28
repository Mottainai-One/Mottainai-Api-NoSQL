package com.mottainai.mongoapi.dto.rag;

import java.time.Instant;

public record RagChunkResponse(
        String id,
        String documentId,
        Integer chunk,
        String text,
        Instant createdAt
) {
}
