package com.cinefinder.chat.controller;

import java.util.List;
import java.util.Map;

import com.cinefinder.chat.data.dto.ChatResponseDto;
import com.cinefinder.chat.data.dto.ChatRequestDto;
import com.cinefinder.chat.service.ChatLogElasticService;
import com.cinefinder.chat.service.ChatRoomService;

import com.cinefinder.chat.service.ChatService;
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
import com.cinefinder.movie.data.repository.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Controller
@RequestMapping("/api")
@Slf4j
public class ChatController {
	private final ChatRoomService chatRoomService;
	private final ChatLogElasticService chatLogElasticService;
	private final ChatService chatService;
	private final MovieRepository movieRepository;

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

	@GetMapping("/chat-log")
	public ResponseEntity<BaseResponse<List<ChatResponseDto>>> getMessages(@ModelAttribute ChatRequestDto chatRequestDto) {
		return ResponseMapper.successOf(ApiStatus._OK, chatService.getChatMessages(chatRequestDto),
			ChatController.class);
	}

	@GetMapping("/chat-sentiment")
	public ResponseEntity<BaseResponse<Map<Long, Double>>> getSentiment() {
		List<Long> movieIds = movieRepository.findAllMovieIds();
		return ResponseMapper.successOf(ApiStatus._OK, chatLogElasticService.getSentimentScores(movieIds),
			ChatController.class);
	}
}