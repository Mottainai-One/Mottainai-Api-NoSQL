package com.mottainai.mongoapi.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mottainai.mongoapi.dto.memory.MemoryResponse;
import com.mottainai.mongoapi.service.MemoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/memories")
@RequiredArgsConstructor
@Tag(name = "Memories", description = "Leitura e exclusão apenas — memória nunca é criada/editada via API")
public class MemoryController {

    private final MemoryService memoryService;

    @GetMapping
    @Operation(summary = "Lista as memórias de longo prazo de uma empresa (paginado, máx. 100 por página)")
    public Page<MemoryResponse> list(
            @RequestParam Long empresaId,
            @PageableDefault(size = 20, sort = "usuarioId") Pageable pageable) {
        return memoryService.list(empresaId, pageable);
    }

    @GetMapping("/{empresaId}/{usuarioId}")
    @Operation(summary = "Busca a memória de um usuário específico")
    public MemoryResponse get(@PathVariable Long empresaId, @PathVariable Long usuarioId) {
        return memoryService.get(empresaId, usuarioId);
    }

    @DeleteMapping("/{empresaId}/{usuarioId}")
    @Operation(summary = "Exclui a memória de um usuário específico")
    public ResponseEntity<Void> delete(@PathVariable Long empresaId, @PathVariable Long usuarioId) {
        memoryService.delete(empresaId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
