package com.mottainai.mongoapi.dto.conversation;

import java.time.Instant;

public record ConversationResponse(
        String sessionId,
        Long empresaId,
        Long usuarioId,
        String agent,
        String skill,
        String status,
        String title,
        Instant startedAt,
        Instant lastInteraction,
        Instant endedAt
) {
}
