package com.cinefinder.user.data.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSignUpRequestDto {
	private String googleSub;
	private String nickname;
}
