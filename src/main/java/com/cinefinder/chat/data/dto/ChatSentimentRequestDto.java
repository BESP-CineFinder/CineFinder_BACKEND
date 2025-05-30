package com.cinefinder.chat.data.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatSentimentRequestDto {
	private List<String> messages;

}
