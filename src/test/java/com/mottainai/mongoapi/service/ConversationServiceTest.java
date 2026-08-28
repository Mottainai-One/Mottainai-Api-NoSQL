package com.mottainai.mongoapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.mottainai.mongoapi.document.Conversation;
import com.mottainai.mongoapi.document.Message;
import com.mottainai.mongoapi.dto.conversation.ConversationResponse;
import com.mottainai.mongoapi.dto.conversation.MessageResponse;
import com.mottainai.mongoapi.exception.ResourceNotFoundException;
import com.mottainai.mongoapi.mapper.ConversationMapper;
import com.mottainai.mongoapi.repository.ConversationRepository;
import com.mottainai.mongoapi.repository.MessageRepository;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long OTHER_EMPRESA_ID = 2L;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        ConversationMapper mapper = Mappers.getMapper(ConversationMapper.class);
        conversationService = new ConversationService(conversationRepository, messageRepository, mapper);
    }

    @Test
    void get_whenConversationBelongsToAnotherEmpresa_throwsNotFound() {
        when(conversationRepository.findBySessionIdAndEmpresaId("session-1", OTHER_EMPRESA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.get("session-1", OTHER_EMPRESA_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void get_whenFound_mapsToResponse() {
        Conversation conversation = Conversation.builder()
                .id("conv-1").sessionId("session-1").empresaId(EMPRESA_ID)
                .usuarioId(10L).status("active").title("Consulta").build();
        when(conversationRepository.findBySessionIdAndEmpresaId("session-1", EMPRESA_ID))
                .thenReturn(Optional.of(conversation));

        ConversationResponse response = conversationService.get("session-1", EMPRESA_ID);

        assertThat(response.sessionId()).isEqualTo("session-1");
        assertThat(response.empresaId()).isEqualTo(EMPRESA_ID);
    }

    @Test
    void listMessages_whenConversationOwned_queriesByInternalIdAndEmpresa() {
        Conversation conversation = Conversation.builder()
                .id("conv-1").sessionId("session-1").empresaId(EMPRESA_ID).status("active").build();
        when(conversationRepository.findBySessionIdAndEmpresaId("session-1", EMPRESA_ID))
                .thenReturn(Optional.of(conversation));

        Message message = Message.builder().id("msg-1").empresaId(EMPRESA_ID).conversationId("conv-1")
                .role("user").content("Olá").build();
        Pageable pageable = PageRequest.of(0, 20);
        when(messageRepository.findByEmpresaIdAndConversationId(EMPRESA_ID, "conv-1", pageable))
                .thenReturn(new PageImpl<>(List.of(message), pageable, 1));

        Page<MessageResponse> result = conversationService.listMessages("session-1", EMPRESA_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).content()).isEqualTo("Olá");
    }

    @Test
    void listMessages_whenConversationNotOwned_throwsNotFoundBeforeQueryingMessages() {
        when(conversationRepository.findBySessionIdAndEmpresaId("session-1", OTHER_EMPRESA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.listMessages("session-1", OTHER_EMPRESA_ID, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
