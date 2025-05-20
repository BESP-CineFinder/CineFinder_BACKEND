package com.cinefinder.chat.service;

import com.cinefinder.chat.data.entity.ChatMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class ChatLogConsumer {

    private final ChatLogElasticService chatLogElasticService;

    @KafkaListener(topicPattern = "chat-log-.*", groupId = "chat-log-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen(ChatMessage messageDto) {
        chatLogElasticService.add(messageDto);
    }
}