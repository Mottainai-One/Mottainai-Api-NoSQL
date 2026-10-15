package com.mottainai.mongoapi.document;

import java.time.Instant;
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

@Document(collection = "metrics")
@CompoundIndexes({
        @CompoundIndex(name = "ix_metrics_conversation_createdAt", def = "{'conversationId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "ix_metrics_agent_skill_createdAt", def = "{'agent': 1, 'skill': 1, 'createdAt': -1}")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metric {

    @Id
    private String id;

    private Long empresaId;
    private String sessionId;
    private String requestId;

    @Field(targetType = FieldType.OBJECT_ID)
    private String conversationId;
    private String agent;
    private String skill;
    private String model;
    private Integer inputTokens;
    private Integer outputTokens;
    private Double latency;
    private Double estimatedCost;
    private CostReference costReference;
    private Double judgeScore;
    private String status;
    private Map<String, Double> nodeLatenciesMs;

    private Instant createdAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostReference {
        private Double inputPerMillionUsd;
        private Double outputPerMillionUsd;
    }
}
