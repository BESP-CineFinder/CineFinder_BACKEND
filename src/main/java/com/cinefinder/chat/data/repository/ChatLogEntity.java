package com.cinefinder.chat.data.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatLogEntity {
    private String id;
    private String senderId;
    private String message;
    private String timestamp;
}
