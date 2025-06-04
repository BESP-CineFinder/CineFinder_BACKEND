package com.cinefinder.chat.data.entity;

import com.cinefinder.global.domain.BaseTimeEntity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage extends BaseTimeEntity {
	private String senderId;

	private String nickName;

	private String movieId;

	private String message;

	private String filteredMessage;

	private ChatType type;

	@Builder.Default
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime createdAt = LocalDateTime.now();

	public void maskMessage(String filteredMessage) {
		this.filteredMessage = filteredMessage;
	}
}