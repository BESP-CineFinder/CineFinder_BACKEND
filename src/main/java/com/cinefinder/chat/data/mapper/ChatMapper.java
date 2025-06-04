package com.cinefinder.chat.data.mapper;

import com.cinefinder.chat.data.dto.ChatResponseDto;
import com.cinefinder.chat.data.entity.ChatLogEntity;
import com.cinefinder.chat.data.entity.ChatMessage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ChatMapper {

    public static ChatResponseDto toChatResponseDto(ChatLogEntity chatLogEntity) {
        LocalDateTime createdAt = Instant.ofEpochMilli(Long.parseLong(chatLogEntity.getCreatedAt()))
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();

        return ChatResponseDto.builder()
                .senderId(chatLogEntity.getSenderId())
                .message(chatLogEntity.getMessage())
                .createdAt(createdAt)
                .build();
    }

    public static ChatResponseDto toChatResponseDto(ChatMessage chatMessage) {
        return ChatResponseDto.builder()
                .senderId(chatMessage.getSenderId())
                .nickName(chatMessage.getNickName())
                .message(chatMessage.getMessage())
                .type(chatMessage.getType())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}
