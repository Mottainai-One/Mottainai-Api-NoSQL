package com.mottainai.mongoapi.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.mottainai.mongoapi.document.Conversation;

public interface ConversationRepository extends MongoRepository<Conversation, String>, ConversationRepositoryCustom {

    Optional<Conversation> findBySessionIdAndEmpresaId(String sessionId, Long empresaId);
}
