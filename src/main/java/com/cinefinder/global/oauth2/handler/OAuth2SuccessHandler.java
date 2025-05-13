package com.cinefinder.global.oauth2.handler;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.cinefinder.global.oauth2.entity.CustomOAuth2User;
import com.cinefinder.global.util.jwt.JwtUtil;
import com.cinefinder.user.data.entity.User;
import com.cinefinder.user.data.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;
	private final RedisTemplate<String, String> redisTemplate; // RedisTemplate 주입
	@Value("${spring.jwt.expiration.refresh}")
	private Long refreshExpiredAge;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
		throws IOException {

		CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
		String kakaoSub = oAuth2User.getKakaoSub();
		String kakaoEmail = oAuth2User.getEmail();

		Optional<User> userOpt = userRepository.findByKakaoSub(kakaoSub);

		if (userOpt.isPresent()) {
			// ✅ SecurityContext에 인증 정보 저장
			UsernamePasswordAuthenticationToken authToken =
				new UsernamePasswordAuthenticationToken(oAuth2User, null, oAuth2User.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authToken);

			// ✅ JWT 발급 및 응답 설정
			// String accessToken = jwtUtil.generateToken(kakaoSub);
			String refreshToken = jwtUtil.generateRefreshToken(kakaoSub);

			redisTemplate.opsForValue().set("RT:" + kakaoSub, refreshToken, Duration.ofDays(1));

			ResponseCookie responseCookie = ResponseCookie.from("Refresh-Token", refreshToken)
				.path("/")
				.httpOnly(true)
				.secure(true)
				.sameSite("None")
				.maxAge(refreshExpiredAge)
				.build();


			response.setHeader("Refresh-Token", refreshToken);
			response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

			response.sendRedirect("/");
		} else {
			// 회원가입 필요시 리다이렉트
			HttpSession session = request.getSession(true);
			session.setAttribute("kakaoSub", kakaoSub);
			session.setAttribute("kakaoEmail", kakaoEmail);

			response.sendRedirect("/signup");
		}
	}

}


