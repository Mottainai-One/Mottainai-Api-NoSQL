package com.mottainai.mongoapi.mapper;

import org.mapstruct.Mapper;

import com.mottainai.mongoapi.document.Conversation;
import com.mottainai.mongoapi.document.Message;
import com.mottainai.mongoapi.dto.conversation.ConversationResponse;
import com.mottainai.mongoapi.dto.conversation.MessageResponse;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    ConversationResponse toResponse(Conversation conversation);

    MessageResponse toResponse(Message message);
}
