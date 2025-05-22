package com.cinefinder.chat.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.chat.service.KafkaService;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Controller
@RequestMapping("/api")
@Slf4j
public class ChatController {
	private final SimpMessagingTemplate messagingTemplate;
	private final KafkaService kafkaService;

	@MessageMapping("/chat-{movieId}")
	public void sendMessage(@DestinationVariable String movieId, ChatMessage message) {
		log.info("Received message: {}", message);
		// kafkaTemplate.send("chat-log-" + message.getMovieId(), null, message);
		kafkaService.sendMessage(message);
		messagingTemplate.convertAndSend("/topic/chat-" + movieId, message);
	}
}
