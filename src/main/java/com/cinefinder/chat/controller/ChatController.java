package com.cinefinder.chat.controller;

import com.cinefinder.chat.service.ChatRoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
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
	private final ChatRoomService chatRoomService;

	@MessageMapping("/chat-{movieId}")
	public void sendMessage(@DestinationVariable String movieId, ChatMessage message) {
		chatRoomService.sendMessage(movieId, message);
	}

	@MessageMapping("/chat-{movieId}/join")
	public void handleJoin(@Header("simpSessionId") String sessionId, @DestinationVariable String movieId, ChatMessage message) {
		chatRoomService.handleJoin(sessionId, movieId, message);
	}

	@MessageMapping("/chat-{movieId}/leave")
	public void handleLeave(@DestinationVariable String movieId, ChatMessage message) {
		chatRoomService.handleLeave(movieId, message);
	}
}