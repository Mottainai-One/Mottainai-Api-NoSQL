package com.mottainai.mongoapi.document;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "memories")
@CompoundIndex(name = "ux_memories_empresa_usuario", def = "{'empresaId': 1, 'usuarioId': 1}", unique = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Memory {

    @Id
    private String id;

    private Long empresaId;
    private Long usuarioId;
    private List<String> preferences;
    private List<String> facts;
    private String lastAgent;
    private String lastSkill;

    @Indexed(name = "ix_memories_updatedAt", direction = IndexDirection.DESCENDING)
    private Instant updatedAt;
}
