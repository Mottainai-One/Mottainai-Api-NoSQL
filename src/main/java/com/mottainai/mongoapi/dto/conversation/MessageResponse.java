package com.mottainai.mongoapi.dto.conversation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MessageResponse(
        String id,
        String requestId,
        String conversationId,
        String role,
        String content,
        String agent,
        String skill,
        String model,
        List<Map<String, Object>> sources,
        Integer inputTokens,
        Integer outputTokens,
        Instant createdAt
) {
}
