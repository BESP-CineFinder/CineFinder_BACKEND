package com.cinefinder.chat.data.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRequestDto {

	private String movieId;
	private LocalDateTime cursorCreatedAt;
	private int size;
}
