package com.cinefinder.chat.data.entity;

import java.time.LocalDateTime;

import com.cinefinder.global.domain.BaseTimeEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage extends BaseTimeEntity {
	private String senderId;

	private String movieId;

	private String message;

	public ChatMessage(String senderId, String movieId, String message, LocalDateTime createdAt) {
		this.senderId = senderId;
		this.movieId = movieId;
		this.message = message;
		super.createdAt(createdAt);
	}
}
