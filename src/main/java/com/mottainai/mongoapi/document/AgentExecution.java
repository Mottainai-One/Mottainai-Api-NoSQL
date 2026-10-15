package com.mottainai.mongoapi.document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "agent_executions")
@CompoundIndexes({
        @CompoundIndex(name = "ix_agent_executions_conversation_startedAt", def = "{'conversationId': 1, 'startedAt': -1}"),
        @CompoundIndex(name = "ix_agent_executions_agent_status_startedAt", def = "{'agent': 1, 'status': 1, 'startedAt': -1}")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecution {

    @Id
    private String id;

    private Long empresaId;
    private String sessionId;
    private String requestId;

    @Field(targetType = FieldType.OBJECT_ID)
    private String conversationId;

    private String agent;
    private String skill;
    private String status;
    private Double latency;
    private Map<String, Double> nodeLatenciesMs;

    /** Ferramentas usadas na execução (ex.: "rag_search", "postgres_query"). */
    private List<String> tools;

    /** Sequência de nós percorridos pelo agente (ex.: "Supervisor" -> "Cliente" -> "Resposta"). */
    private List<String> executionFlow;

    /**
     * A IA Layer passou a gravar o erro como documento estruturado (ex.: {"message": "APIStatusError"}),
     * e não mais como string — mapear como String devolvia o JSON escapado dentro de uma string.
     */
    private Map<String, Object> error;

    private Instant startedAt;
    private Instant finishedAt;
    private Instant createdAt;
}
