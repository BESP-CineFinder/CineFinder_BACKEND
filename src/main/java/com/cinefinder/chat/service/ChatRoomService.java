package com.cinefinder.chat.service;

import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.chat.data.entity.ChatType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {
    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;
    private final KafkaService kafkaService;
    private final ChatLogElasticService chatLogElasticService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisSessionService redisSessionService;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String CHAT_ROOM_PREFIX = "chat:room:";
    private static final String CHAT_ROOM_PARTICIPANTS = ":participants";

    public void sendMessage(String movieId, ChatMessage message) {
        // 메시지 타입에 따른 처리
        if (message.getType() == ChatType.CHAT) {
            // 일반 채팅 메시지 처리
            kafkaService.createTopicIfNotExists(message.getMovieId());
            kafkaTemplate.send("chat-log-" + message.getMovieId(), message.getSenderId(), message);
            chatLogElasticService.save(message);
            log.info("Save chat message: {}", message);
        }
        messagingTemplate.convertAndSend("/topic/chat-" + movieId, message);
    }

    public void handleJoin(String sessionId, String movieId, ChatMessage message) {
        log.info("User joined: {}", message.getNickName());

        Long userId = Long.valueOf(message.getSenderId());
        String nickname = message.getNickName();

        redisSessionService.registerSessionInfo(sessionId, userId, movieId, nickname);

        // Redis 참여자 추가
        addParticipant(movieId, message.getNickName());

        // 시스템 메시지 생성
        ChatMessage systemMessage = ChatMessage.builder()
                .type(ChatType.SYSTEM)
                .movieId(movieId)
                .message(message.getNickName() + "님이 입장하셨습니다.")
                .build();

        // 시스템 메시지 전송
        messagingTemplate.convertAndSend("/topic/chat-" + movieId, systemMessage);

        // 참여자 목록 전송
        Set<String> participants = getParticipants(movieId);
        messagingTemplate.convertAndSend("/topic/chat-" + movieId, new ArrayList<>(participants));
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