package com.cinefinder.chat.data.dto;

import lombok.*;

@Data
public class ChatMessageDto {
    private String id;
    private String senderId;
    private String message;
}
