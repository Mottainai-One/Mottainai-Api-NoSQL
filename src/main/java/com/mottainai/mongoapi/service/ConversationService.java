package com.mottainai.mongoapi.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.mottainai.mongoapi.document.Conversation;
import com.mottainai.mongoapi.dto.conversation.ConversationResponse;
import com.mottainai.mongoapi.dto.conversation.MessageResponse;
import com.mottainai.mongoapi.exception.ResourceNotFoundException;
import com.mottainai.mongoapi.mapper.ConversationMapper;
import com.mottainai.mongoapi.repository.ConversationRepository;
import com.mottainai.mongoapi.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapper mapper;

    public Page<ConversationResponse> list(Long empresaId, Long usuarioId, String status, Pageable pageable) {
        return conversationRepository.search(empresaId, usuarioId, status, pageable).map(mapper::toResponse);
    }

    public ConversationResponse get(String sessionId, Long empresaId) {
        return mapper.toResponse(findOwnedConversation(sessionId, empresaId));
    }

    public Page<MessageResponse> listMessages(String sessionId, Long empresaId, Pageable pageable) {
        Conversation conversation = findOwnedConversation(sessionId, empresaId);
        return messageRepository.findByEmpresaIdAndConversationId(empresaId, conversation.getId(), pageable)
                .map(mapper::toResponse);
    }

    private Conversation findOwnedConversation(String sessionId, Long empresaId) {
        return conversationRepository.findBySessionIdAndEmpresaId(sessionId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada: " + sessionId));
    }
}
