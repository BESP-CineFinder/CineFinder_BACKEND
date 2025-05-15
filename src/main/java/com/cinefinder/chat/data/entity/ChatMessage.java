package com.cinefinder.chat.data.entity;

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
	private Long senderId;

	private Long movieId;

	private String message;
}
