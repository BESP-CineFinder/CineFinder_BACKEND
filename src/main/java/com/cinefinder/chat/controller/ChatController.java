package com.cinefinder.chat.controller;

import com.cinefinder.chat.data.dto.ChatMessageDto;
import com.cinefinder.chat.service.ChatLogElasticService;
import com.cinefinder.chat.service.KafkaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
    private final KafkaTemplate<String, ChatMessageDto> kafkaTemplate;
    private final KafkaService kafkaService;
    private final ChatLogElasticService chatLogElasticService;

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody ChatMessageDto chatMessageDto) {
        kafkaService.createTopicIfNotExists(chatMessageDto.getId()); // 동적으로 토픽 생성
        kafkaTemplate.send("chat-log-" + chatMessageDto.getId(), chatMessageDto.getSenderId(), chatMessageDto);
        chatLogElasticService.save(chatMessageDto);
        return ResponseEntity.ok("메시지 전송 완료");
    }
}
