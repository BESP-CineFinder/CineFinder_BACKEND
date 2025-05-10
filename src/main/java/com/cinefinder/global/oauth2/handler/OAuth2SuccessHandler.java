package com.cinefinder.global.oauth2.handler;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.cinefinder.global.oauth2.entity.CustomOAuth2User;
import com.cinefinder.global.util.jwt.JwtUtil;
import com.cinefinder.user.data.entity.User;
import com.cinefinder.user.data.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;
	private final RedisTemplate<String, String> redisTemplate; // RedisTemplate 주입

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
		throws IOException {

		CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
		String googleSub = oAuth2User.getGoogleSub();

		Optional<User> userOpt = userRepository.findByGoogleSub(googleSub);

		if (userOpt.isPresent()) {
			String accessToken = jwtUtil.generateToken(googleSub); // access token 발급
			String refreshToken = jwtUtil.generateRefreshToken(googleSub); // refresh token 발급

			// refresh token을 Redis에 저장 (redis에서 만료시간을 설정해줌)
			redisTemplate.opsForValue().set("RT:" + googleSub, refreshToken, Duration.ofDays(1)); // 예시로 7일 설정

			response.setHeader("Authorization", "Bearer " + accessToken);
			response.setHeader("Refresh-Token", refreshToken); // refresh token 헤더로 전달
		} else {
			// 사용자가 가입되지 않은 경우, 회원가입 페이지로 리다이렉트
			response.sendRedirect("/signup?googleSub=" + googleSub);
		}
	}
}


