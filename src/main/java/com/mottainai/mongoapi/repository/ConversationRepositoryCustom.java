package com.mottainai.mongoapi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mottainai.mongoapi.document.Conversation;

public interface ConversationRepositoryCustom {

    Page<Conversation> search(Long empresaId, Long usuarioId, String status, Pageable pageable);
}
