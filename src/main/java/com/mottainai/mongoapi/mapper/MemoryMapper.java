package com.mottainai.mongoapi.mapper;

import org.mapstruct.Mapper;

import com.mottainai.mongoapi.document.Memory;
import com.mottainai.mongoapi.dto.memory.MemoryResponse;

@Mapper(componentModel = "spring")
public interface MemoryMapper {

    MemoryResponse toResponse(Memory memory);
}
