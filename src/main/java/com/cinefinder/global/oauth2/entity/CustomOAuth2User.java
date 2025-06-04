package com.cinefinder.global.oauth2.entity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
@Getter
public class CustomOAuth2User implements OAuth2User, Serializable {

	private static final long serialVersionUID = 1L;
	private final String kakaoSub;
	private final String email;
	private final Map<String, Object> attributes;

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(); // 권한은 JWT 필터에서 부여
	}

	@Override
	public String getName() {
		return kakaoSub;
	}
}
