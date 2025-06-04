package com.cinefinder.global.util.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

	private final RedisTemplate<String, String> redisTemplate;

	private static final String REFRESH_PREFIX = "RT:";
	private static final String BLACKLIST_PREFIX = "BL:";

	// 저장
	public void saveRefreshToken(String username, String refreshToken, long expirationMillis) {
		redisTemplate.opsForValue().set(REFRESH_PREFIX + username, refreshToken, Duration.ofMillis(expirationMillis));
	}

	// 가져오기
	public String getRefreshToken(String username) {
		return redisTemplate.opsForValue().get(REFRESH_PREFIX + username);
	}

	// 삭제
	public void deleteRefreshToken(String username) {
		redisTemplate.delete(REFRESH_PREFIX + username);
	}

	// 블랙리스트 등록
	public void blacklistAccessToken(String accessToken, long expirationMillis) {
		redisTemplate.opsForValue().set(BLACKLIST_PREFIX + accessToken, "logout", Duration.ofMillis(expirationMillis));
	}

	// 블랙리스트 여부 확인
	public boolean isBlacklisted(String accessToken) {
		return redisTemplate.hasKey(BLACKLIST_PREFIX + accessToken);
	}
}
