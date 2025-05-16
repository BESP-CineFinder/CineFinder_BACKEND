package com.cinefinder.chat.data.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatLogEntity {
    private String id;
    private String senderId;
    private String message;
    private String timestamp;
}
