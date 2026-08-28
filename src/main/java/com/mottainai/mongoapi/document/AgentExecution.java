package com.mottainai.mongoapi.document;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "agent_executions")
@CompoundIndexes({
        @CompoundIndex(name = "empresaId_createdAt_idx", def = "{'empresaId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "agent_status_idx", def = "{'agent': 1, 'status': 1}")
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
    private String agent;
    private String status;
    private Double latency;
    private Map<String, Double> nodeLatenciesMs;
    private String error;
    private Instant startedAt;
    private Instant createdAt;
}
