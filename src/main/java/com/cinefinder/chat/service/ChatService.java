package com.cinefinder.chat.service;

import com.cinefinder.chat.data.dto.reponse.ChatResponseDto;
import com.cinefinder.chat.data.dto.request.ChatRequestDto;
import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.chat.data.entity.ChatType;
import com.cinefinder.chat.data.mapper.ChatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatLogElasticService chatElasticService;
    private final RedisSessionService chatSessionService;

    public List<ChatResponseDto> getChatMessages(ChatRequestDto chatRequestDto) {
        String movieId = chatRequestDto.getMovieId();
        LocalDateTime cursorCreatedAt = (chatRequestDto.getCursorCreatedAt() == null) ? null : chatRequestDto.getCursorCreatedAt().atZone(ZoneOffset.UTC).toLocalDateTime();
        int size = chatRequestDto.getSize();

        List<ChatMessage> redisMessages = chatSessionService.getMessagesFromRedis(movieId, cursorCreatedAt, size);

        int remaining = size - redisMessages.size();
        List<ChatMessage> esMessages = new ArrayList<>();
        if (remaining > 0) {
            esMessages = chatElasticService.getMessagesFromElasticsearch(movieId, cursorCreatedAt, remaining);
        }

        List<ChatResponseDto> combined = new ArrayList<>();
        combined.addAll(redisMessages.stream().map(ChatMapper::toChatResponseDto).toList());
        combined.addAll(esMessages.stream().map(ChatMapper::toChatResponseDto).toList());

        return combined.stream()
                .distinct()
                .map(dto -> {
                    LocalDateTime seoulTime = dto.getCreatedAt()
                            .atZone(ZoneOffset.UTC)
                            .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
                            .toLocalDateTime();
                    return ChatResponseDto.builder()
                            .senderId(dto.getSenderId())
                            .nickName(dto.getNickName())
                            .message(dto.getMessage())
                            .type(ChatType.PREV)
                            .createdAt(seoulTime)
                            .build();
                })
                .sorted(Comparator.comparing(ChatResponseDto::getCreatedAt).reversed())
                .limit(size)
                .collect(Collectors.toList());
    }
}
