package com.cinefinder.chat.controller;

import com.cinefinder.chat.data.dto.ChatMessageDto;
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

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody ChatMessageDto message) {
        kafkaTemplate.send("chat-log", message);
        return ResponseEntity.ok("메시지 전송 완료");
    }
}
