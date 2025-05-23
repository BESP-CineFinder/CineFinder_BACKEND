package com.cinefinder.chat.service;

import com.cinefinder.chat.data.dto.SessionInfo;
import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisSessionService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SESSION_PREFIX = "session:";

    public void removeSession(String sessionId) {
        redisTemplate.delete(SESSION_PREFIX + sessionId);
    }

    public void registerSessionInfo(String sessionId, Long userId, String movieId, String nickname) {
        SessionInfo info = new SessionInfo(userId, movieId, nickname);
        try {
            String json = objectMapper.writeValueAsString(info);
            redisTemplate.opsForValue().set("session:" + sessionId, json);
        } catch (Exception e) {
            throw new CustomException(ApiStatus._REDIS_SAVE_FAIL, "채팅용 Session 저장 실패");
        }
    }

    public SessionInfo getSessionInfo(String sessionId) {
        try {
            String json = redisTemplate.opsForValue().get("session:" + sessionId);
            return objectMapper.readValue(json, SessionInfo.class);
        } catch (Exception e) {
            throw new CustomException(ApiStatus._REDIS_CHECK_FAIL);
        }
    }
}