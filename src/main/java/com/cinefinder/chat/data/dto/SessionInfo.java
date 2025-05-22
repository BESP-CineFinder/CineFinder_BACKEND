package com.cinefinder.chat.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionInfo {
    private Long userId;
    private String movieId;
    private String nickname;
}
