package com.cinefinder.chat.controller;

import com.cinefinder.chat.data.dto.ChatMessageDto;
import com.cinefinder.chat.data.repository.ChatLogEntity;
import com.cinefinder.chat.service.ChatLogElasticService;
import com.cinefinder.chat.service.KafkaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat/log")
@RequiredArgsConstructor
public class ChatLogQueryController {

    private final ChatLogElasticService chatLogElasticService;
    private final KafkaService logFetcher;

    @GetMapping("/all/{movieId}")
    public List<ChatLogEntity> getAllLogs(@PathVariable String movieId) {
        return chatLogElasticService.findAll(movieId);
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<List<ChatMessageDto>> getLogsByMovieId(@PathVariable String movieId) {
        List<ChatMessageDto> logs = logFetcher.fetchLogsByMovieId(movieId);
        return ResponseEntity.ok(logs);
    }
}