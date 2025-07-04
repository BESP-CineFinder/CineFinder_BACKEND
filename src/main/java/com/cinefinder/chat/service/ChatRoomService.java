package com.cinefinder.chat.service;

import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.chat.data.entity.ChatType;
import com.cinefinder.global.util.service.BadWordFilterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {
    private final KafkaService kafkaService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisSessionService redisSessionService;
    private final RedisTemplate<String, String> redisTemplate;
    private final BadWordFilterService badWordFilterService;
    private static final String CHAT_ROOM_PREFIX = "chat:room:";
    private static final String CHAT_ROOM_PARTICIPANTS = ":participants";

    public void sendMessage(String movieId, ChatMessage message) {
        // 메시지 타입에 따른 처리
        if (message.getType() == ChatType.CHAT) {
            // 메세지 필터링 처리
            String filteredMessage = badWordFilterService.maskBadWords(message.getMessage());
            // 일반 채팅 메시지 처리
            message.maskMessage(filteredMessage);
            kafkaService.sendMessage(message);
            // redis에 5초 캐싱 메세지 데이터 저장
            redisSessionService.cacheMessage(movieId, message);
            log.info("Save chat message: {}", message);
            messagingTemplate.convertAndSend("/topic/chat-" + movieId, message);
        }
    }

    public void handleJoin(String sessionId, String movieId, ChatMessage message) {
        CompletableFuture.runAsync(() -> {
            try {
                Long userId = Long.valueOf(message.getSenderId());
                String nickname = message.getNickName();

                redisSessionService.registerSessionInfo(sessionId, userId, movieId, nickname);

                addParticipant(movieId, nickname); // Redis Set 사용

                ChatMessage systemMessage = ChatMessage.builder()
                        .type(ChatType.SYSTEM)
                        .movieId(movieId)
                        .message(nickname + "님이 입장하셨습니다.")
                        .build();

                messagingTemplate.convertAndSend("/topic/chat-" + movieId, systemMessage);

                Set<String> participants = getParticipants(movieId);
                messagingTemplate.convertAndSend("/topic/chat-" + movieId, new ArrayList<>(participants));

            } catch (Exception e) {
                log.error("비동기 handleJoin 처리 중 오류", e);
            }
        });
    }

    public void handleLeave(String movieId, ChatMessage message) {
        log.info("User left: {}", message.getNickName());

        // Redis에서 참여자 제거
        removeParticipant(movieId, message.getNickName());

        // 시스템 메시지 생성
        ChatMessage systemMessage = ChatMessage.builder()
                .type(ChatType.SYSTEM)
                .movieId(movieId)
                .message(message.getNickName() + "님이 퇴장하셨습니다.")
                .build();

        // 시스템 메시지 전송
        messagingTemplate.convertAndSend("/topic/chat-" + movieId, systemMessage);

        // 참여자 목록 전송
        Set<String> participants = getParticipants(movieId);
        messagingTemplate.convertAndSend("/topic/chat-" + movieId, new ArrayList<>(participants));
    }

    public void addParticipant(String movieId, String nickname) {
        String key = CHAT_ROOM_PREFIX + movieId + CHAT_ROOM_PARTICIPANTS;
        redisTemplate.opsForSet().add(key, nickname);
        log.info("Added participant {} to room {}", nickname, movieId);
    }

    public void removeParticipant(String movieId, String nickname) {
        String key = CHAT_ROOM_PREFIX + movieId + CHAT_ROOM_PARTICIPANTS;
        redisTemplate.opsForSet().remove(key, nickname);
        log.info("Removed participant {} from room {}", nickname, movieId);

        Long size = redisTemplate.opsForSet().size(key);
        if (size == null || size == 0) {
            redisTemplate.delete(key);
            log.info("Deleted room {} as it's now empty", movieId);
        }
    }

    public Set<String> getParticipants(String movieId) {
        String key = CHAT_ROOM_PREFIX + movieId + CHAT_ROOM_PARTICIPANTS;
        return redisTemplate.opsForSet().members(key);
    }
}