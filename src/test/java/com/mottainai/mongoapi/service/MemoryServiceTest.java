package com.mottainai.mongoapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mottainai.mongoapi.document.Memory;
import com.mottainai.mongoapi.dto.memory.MemoryResponse;
import com.mottainai.mongoapi.exception.ResourceNotFoundException;
import com.mottainai.mongoapi.mapper.MemoryMapper;
import com.mottainai.mongoapi.repository.MemoryRepository;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long USUARIO_ID = 10L;

    @Mock
    private MemoryRepository memoryRepository;

    private MemoryService memoryService;

    @BeforeEach
    void setUp() {
        MemoryMapper mapper = Mappers.getMapper(MemoryMapper.class);
        memoryService = new MemoryService(memoryRepository, mapper);
    }

    @Test
    void get_whenNotFound_throwsNotFound() {
        when(memoryRepository.findByEmpresaIdAndUsuarioId(EMPRESA_ID, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.get(EMPRESA_ID, USUARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void get_whenFound_mapsToResponse() {
        Memory memory = Memory.builder().empresaId(EMPRESA_ID).usuarioId(USUARIO_ID)
                .preferences(java.util.List.of("relatórios compactos")).build();
        when(memoryRepository.findByEmpresaIdAndUsuarioId(EMPRESA_ID, USUARIO_ID)).thenReturn(Optional.of(memory));

        MemoryResponse response = memoryService.get(EMPRESA_ID, USUARIO_ID);

        assertThat(response.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(response.preferences()).containsExactly("relatórios compactos");
    }

    @Test
    void delete_whenFound_delegatesDeleteToRepository() {
        Memory memory = Memory.builder().empresaId(EMPRESA_ID).usuarioId(USUARIO_ID).build();
        when(memoryRepository.findByEmpresaIdAndUsuarioId(EMPRESA_ID, USUARIO_ID)).thenReturn(Optional.of(memory));

        memoryService.delete(EMPRESA_ID, USUARIO_ID);

        verify(memoryRepository, times(1)).deleteByEmpresaIdAndUsuarioId(EMPRESA_ID, USUARIO_ID);
    }

    @Test
    void delete_whenNotFound_throwsNotFoundAndNeverDeletes() {
        when(memoryRepository.findByEmpresaIdAndUsuarioId(EMPRESA_ID, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.delete(EMPRESA_ID, USUARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(memoryRepository, never()).deleteByEmpresaIdAndUsuarioId(EMPRESA_ID, USUARIO_ID);
    }
}
