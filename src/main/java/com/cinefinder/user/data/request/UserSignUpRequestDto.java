package com.cinefinder.user.data.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSignUpRequestDto {
	private String kakaoSub;
	private String kakaoEmail;
	private String nickname;
}
