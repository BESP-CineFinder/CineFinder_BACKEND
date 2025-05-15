package com.cinefinder.chat.service;

import com.cinefinder.chat.data.dto.ChatMessageDto;
import com.cinefinder.chat.data.repository.ChatLogEntity;
import com.cinefinder.chat.data.repository.ChatLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLogElasticService {

    private final ChatLogRepository chatLogRepository;

    public void save(ChatMessageDto dto) {
        ChatLogEntity entity = ChatLogEntity.builder()
                .id(dto.getId())
                .senderId(dto.getSenderId())
                .message(dto.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        chatLogRepository.save(entity);
    }

    public List<ChatLogEntity> findAll() {
        Iterable<ChatLogEntity> iterable = chatLogRepository.findAll();
        List<ChatLogEntity> list = new ArrayList<>();
        iterable.forEach(list::add);

        return list;
    }
}