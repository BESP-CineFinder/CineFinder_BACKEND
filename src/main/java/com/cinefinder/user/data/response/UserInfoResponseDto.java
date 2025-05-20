package com.cinefinder.user.data.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserInfoResponseDto {
	private Long userId;
	private String nickname;
}
