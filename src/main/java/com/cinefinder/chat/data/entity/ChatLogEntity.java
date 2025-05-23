package com.cinefinder.chat.data.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
