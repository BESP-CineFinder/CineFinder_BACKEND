package com.cinefinder.chat.service;

import com.cinefinder.chat.data.dto.ChatMessageDto;
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
    public void consume(ChatMessageDto messageDto) {
        log.info("Kafka received: {}", messageDto);
        chatLogElasticService.save(messageDto);
    }
}