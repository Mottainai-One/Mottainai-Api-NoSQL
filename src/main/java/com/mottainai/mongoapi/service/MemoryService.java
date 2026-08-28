package com.mottainai.mongoapi.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.mottainai.mongoapi.document.Memory;
import com.mottainai.mongoapi.dto.memory.MemoryResponse;
import com.mottainai.mongoapi.exception.ResourceNotFoundException;
import com.mottainai.mongoapi.mapper.MemoryMapper;
import com.mottainai.mongoapi.repository.MemoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final MemoryMapper mapper;

    public Page<MemoryResponse> list(Long empresaId, Pageable pageable) {
        return memoryRepository.findByEmpresaId(empresaId, pageable).map(mapper::toResponse);
    }

    public MemoryResponse get(Long empresaId, Long usuarioId) {
        return mapper.toResponse(findOwnedMemory(empresaId, usuarioId));
    }

    public void delete(Long empresaId, Long usuarioId) {
        findOwnedMemory(empresaId, usuarioId);
        memoryRepository.deleteByEmpresaIdAndUsuarioId(empresaId, usuarioId);
    }

    private Memory findOwnedMemory(Long empresaId, Long usuarioId) {
        return memoryRepository.findByEmpresaIdAndUsuarioId(empresaId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Memória não encontrada para empresaId=" + empresaId + ", usuarioId=" + usuarioId));
    }
}
