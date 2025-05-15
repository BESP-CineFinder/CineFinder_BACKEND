package com.cinefinder.chat.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cinefinder.chat.data.entity.ChatMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Controller
@RequestMapping("/api")
@Slf4j
public class ChatController {

	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/chat-{movieId}")
	public void sendMessage(@DestinationVariable String movieId, ChatMessage message) {
		log.info("Received message: {}", message);
		messagingTemplate.convertAndSend("/topic/chat-" + movieId, message);
	}
}
