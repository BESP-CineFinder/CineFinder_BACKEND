package com.cinefinder.chat.data.dto;

import com.cinefinder.chat.data.entity.ChatType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class ChatResponseDto {
    private String senderId;
    private String nickName;
    private String message;
    private ChatType type;
    private LocalDateTime createdAt;
}
