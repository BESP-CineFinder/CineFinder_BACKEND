package com.cinefinder.chat.data.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
public class ChatSentimentRequestDto {
	private List<String> messages;

}
