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

@Document(collection = "messages")
@CompoundIndexes({
        @CompoundIndex(name = "ix_messages_conversation_createdAt", def = "{'conversationId': 1, 'createdAt': 1}"),
        @CompoundIndex(name = "ix_messages_agent_skill_createdAt", def = "{'agent': 1, 'skill': 1, 'createdAt': -1}")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private String id;

    private Long empresaId;

    @Field(targetType = FieldType.OBJECT_ID)
    private String conversationId;
    private String role;
    private String content;
    private String agent;
    private String skill;
    private String model;
    private List<Map<String, Object>> sources;
    private Integer inputTokens;
    private Integer outputTokens;
    private Instant createdAt;
}
