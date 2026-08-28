package com.mottainai.mongoapi.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mottainai.mongoapi.dto.conversation.ConversationResponse;
import com.mottainai.mongoapi.dto.conversation.MessageResponse;
import com.mottainai.mongoapi.service.ConversationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Somente leitura — escrito exclusivamente pela IA Layer")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    @Operation(summary = "Lista conversas de uma empresa, opcionalmente filtradas por usuário e status")
    public Page<ConversationResponse> list(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "startedAt") Pageable pageable) {
        return conversationService.list(empresaId, usuarioId, status, pageable);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Busca uma conversa pelo sessionId, restrita à empresa informada")
    public ConversationResponse get(@PathVariable String sessionId, @RequestParam Long empresaId) {
        return conversationService.get(sessionId, empresaId);
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "Lista as mensagens de uma conversa, restrita à empresa informada")
    public Page<MessageResponse> listMessages(
            @PathVariable String sessionId,
            @RequestParam Long empresaId,
            @PageableDefault(sort = "createdAt") Pageable pageable) {
        return conversationService.listMessages(sessionId, empresaId, pageable);
    }
}
