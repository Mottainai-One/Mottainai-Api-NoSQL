package com.mottainai.mongoapi.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.mottainai.mongoapi.document.RagChunk;
import com.mottainai.mongoapi.document.RagDocument;
import com.mottainai.mongoapi.dto.rag.RagChunkResponse;
import com.mottainai.mongoapi.dto.rag.RagDocumentRequest;
import com.mottainai.mongoapi.dto.rag.RagDocumentResponse;

@Mapper(componentModel = "spring")
public interface RagMapper {

    RagDocumentResponse toResponse(RagDocument document);

    /** Sem campo "embedding" no destino: RagChunkResponse não o declara. */
    RagChunkResponse toResponse(RagChunk chunk);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RagDocument toEntity(RagDocumentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "empresaId", ignore = true)
    void updateEntity(RagDocumentRequest request, @MappingTarget RagDocument target);
}
