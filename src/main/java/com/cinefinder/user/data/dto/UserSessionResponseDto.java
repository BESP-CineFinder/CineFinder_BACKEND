package com.cinefinder.user.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class UserSessionResponseDto {
	private String kakaoSub;
	private String kakaoEmail;

}
