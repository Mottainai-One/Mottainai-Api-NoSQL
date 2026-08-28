package com.mottainai.mongoapi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.mottainai.mongoapi.document.Message;

public interface MessageRepository extends MongoRepository<Message, String> {

    Page<Message> findByEmpresaIdAndConversationId(Long empresaId, String conversationId, Pageable pageable);
}
