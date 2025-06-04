package com.cinefinder.chat.controller;

import com.cinefinder.chat.data.entity.ChatLogEntity;
import com.cinefinder.chat.service.ChatLogElasticService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/all/{movieId}")
    public List<ChatLogEntity> getAllLogs(@PathVariable String movieId) {
        return chatLogElasticService.findAll(movieId);
    }
}