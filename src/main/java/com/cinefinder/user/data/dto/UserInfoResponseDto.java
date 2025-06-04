package com.cinefinder.user.data.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserInfoResponseDto {
	private Long userId;
	private String nickname;
}
