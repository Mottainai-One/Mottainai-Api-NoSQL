package com.mottainai.mongoapi.dto.memory;

import java.time.Instant;
import java.util.List;

public record MemoryResponse(
        Long empresaId,
        Long usuarioId,
        List<String> preferences,
        List<String> facts,
        String lastAgent,
        String lastSkill,
        Instant updatedAt
) {
}
