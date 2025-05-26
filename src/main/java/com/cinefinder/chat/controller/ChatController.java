package com.cinefinder.chat.controller;

import java.util.List;

import com.cinefinder.chat.data.dto.reponse.ChatResponseDto;
import com.cinefinder.chat.data.dto.request.ChatRequestDto;
import com.cinefinder.chat.service.ChatLogElasticService;
import com.cinefinder.chat.service.ChatRoomService;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cinefinder.chat.data.entity.ChatMessage;

import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Controller
@RequestMapping("/api")
@Slf4j
public class ChatController {
	private final ChatRoomService chatRoomService;
	private final ChatLogElasticService chatLogElasticService;

	@MessageMapping("/chat-{movieId}")
	public void sendMessage(@DestinationVariable String movieId, ChatMessage message) {
		log.info("Received message: {}", message);
		chatRoomService.sendMessage(movieId, message);
	}

	@MessageMapping("/chat-{movieId}/join")
	public void handleJoin(@Header("simpSessionId") String sessionId, @DestinationVariable String movieId,
		ChatMessage message) {
		chatRoomService.handleJoin(sessionId, movieId, message);
	}

	@MessageMapping("/chat-{movieId}/leave")
	public void handleLeave(@DestinationVariable String movieId, ChatMessage message) {
		chatRoomService.handleLeave(movieId, message);
	}

	// 채팅 로그 불러올 때, 채팅 데이터 개수를 제한하고, Redis로부터 5초이내의 데이터를 확인하고 부족한 개수만큼
	// Elasticsearch로 부터 개수를 맞춰서 불러오기
	@GetMapping("/chat-log")
	public ResponseEntity<BaseResponse<List<ChatResponseDto>>> getMessages(@ModelAttribute ChatRequestDto chatRequestDto) {
		return ResponseMapper.successOf(ApiStatus._OK, chatLogElasticService.getMessages(chatRequestDto),
			ChatController.class);
	}
}