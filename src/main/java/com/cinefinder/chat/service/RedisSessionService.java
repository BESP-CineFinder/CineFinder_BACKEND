package com.cinefinder.chat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cinefinder.chat.data.dto.SessionInfo;
import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.chat.data.entity.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSessionService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String SESSION_PREFIX = "session:";
    private static final String CHAT_PREFIX = "chat:cache:";
    private static final String LOCK_PREFIX = "lock:chat:cache:";

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

    public void cacheMessage(String movieId, ChatMessage message) {
        try {
            String key = CHAT_PREFIX + movieId;

            String jsonMessage = objectMapper.writeValueAsString(message);

            RList<String> chatList = redissonClient.getList(key);
            chatList.add(jsonMessage);

        } catch (Exception e) {
            throw new CustomException(ApiStatus._REDIS_SAVE_FAIL, "Redis Chat Message 저장 실패");
        }
    }

    public List<ChatMessage> getMessagesFromRedis(String movieId, LocalDateTime cursorCreatedAt, int size) {
        String key = CHAT_PREFIX + movieId;
        RList<String> chatList = redissonClient.getList(key);

        List<ChatMessage> result = new ArrayList<>();
        LocalDateTime lastCreatedAt = null;
        for (int i = chatList.size() - 1; i >= 0; i--) {
            try {
                ChatMessage msg = objectMapper.readValue(chatList.get(i), ChatMessage.class);

                if (cursorCreatedAt == null || msg.getCreatedAt().isBefore(cursorCreatedAt)) {
                    if (result.size() < size) {
                        result.add(msg);
                        lastCreatedAt = msg.getCreatedAt();
                    } else if (lastCreatedAt != null && msg.getCreatedAt().isEqual(lastCreatedAt)) {
                        result.add(msg);
                    } else {
                        break;
                    }
                }

            } catch (Exception e) {
                log.error("Redis에서 메시지 변환 실패: {}", chatList.get(i), e);
            }
        }
        return result;
    }

    public void clearCachedMessages(String movieId) {
        String lockKey = LOCK_PREFIX + movieId;
        String redisKey = CHAT_PREFIX + movieId;

        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(1, 3, TimeUnit.SECONDS);
            if (isLocked) {
                redissonClient.getList(redisKey).clear();
            } else {
                log.warn("[Message cache clear] Lock 획득 실패: 다른 인스턴스가 메시지를 처리 중입니다. movieId={}", movieId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("[Message cache clear] Redis 분산 락 획득 중 인터럽트 발생", e);
        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }
    }

}