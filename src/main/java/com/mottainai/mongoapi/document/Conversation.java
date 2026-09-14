package com.mottainai.mongoapi.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "conversations")
@CompoundIndexes({
        @CompoundIndex(name = "ix_conversations_empresa_usuario_startedAt", def = "{'empresaId': 1, 'usuarioId': 1, 'startedAt': -1}"),
        @CompoundIndex(name = "ix_conversations_agent_status", def = "{'agent': 1, 'status': 1}")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    private String id;

    @Indexed(name = "ux_conversations_sessionId", unique = true)
    private String sessionId;

    private Long empresaId;
    private Long usuarioId;
    private String agent;
    private String skill;
    private String status;
    private String title;
    private Instant startedAt;

    @Indexed(name = "ix_conversations_lastInteraction", direction = IndexDirection.DESCENDING)
    private Instant lastInteraction;

    private Instant endedAt;
}
