package com.mottainai.mongoapi.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.mottainai.mongoapi.document.Memory;

public interface MemoryRepository extends MongoRepository<Memory, String> {

    Page<Memory> findByEmpresaId(Long empresaId, Pageable pageable);

    Optional<Memory> findByEmpresaIdAndUsuarioId(Long empresaId, Long usuarioId);

    void deleteByEmpresaIdAndUsuarioId(Long empresaId, Long usuarioId);
}
