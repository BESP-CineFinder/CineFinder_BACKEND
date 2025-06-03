package com.cinefinder.chat.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SessionInfo {
    private Long userId;
    private String movieId;
    private String nickname;
}