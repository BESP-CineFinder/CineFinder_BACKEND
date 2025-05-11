package com.cinefinder.global.filter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cinefinder.global.util.jwt.JwtUtil;
import com.cinefinder.global.util.service.TokenService;
import com.cinefinder.user.data.entity.User;
import com.cinefinder.user.data.repository.UserRepository;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;
	private final TokenService tokenService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		String token = resolveToken(request);
		if (token != null) {
			// ✅ 블랙리스트 체크 먼저
			if (tokenService.isBlacklisted(token)) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is blacklisted");
				return;
			}

			try {
				if (jwtUtil.validateToken(token)) {
					String kakaoSub = jwtUtil.getUsernameFromToken(token);
					Optional<User> userOpt = userRepository.findByKakaoSub(kakaoSub);

					if (userOpt.isPresent()) {
						User user = userOpt.get();
						UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
							user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
						);
						SecurityContextHolder.getContext().setAuthentication(auth);
						log.info("Authenticated user: {}", user.getNickname());
					} else {
						response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found. Please complete signup.");
						return;
					}
				} else {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token.");
					return;
				}
			} catch (ExpiredJwtException e) {
				handleExpiredAccessToken(token, response);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		return (bearerToken != null && bearerToken.startsWith("Bearer "))
			? bearerToken.substring(7)
			: bearerToken;
	}

	private void handleExpiredAccessToken(String accessToken, HttpServletResponse response) throws IOException {
		// Refresh Token은 별도 헤더에서 꺼냄
		String refreshToken = resolveRefreshToken(response);
		if (refreshToken == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token missing. Please login again.");
			return;
		}

		try {
			Map<String, String> newTokens = jwtUtil.refreshToken(refreshToken);
			String newAccessToken = newTokens.get("accessToken");
			String newRefreshToken = newTokens.get("refreshToken");

			// 새 토큰을 응답 헤더에 설정
			response.setHeader("Authorization", "Bearer " + newAccessToken);
			response.setHeader("Refresh-Token", "Bearer " + newRefreshToken);
		} catch (RuntimeException e) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token expired. Please login again.");
		}
	}

	private String resolveRefreshToken(HttpServletResponse response) {
		HttpServletRequest request = ((org.apache.catalina.connector.Response) response).getRequest();
		String refreshHeader = request.getHeader("Refresh-Token");
		return (refreshHeader != null && refreshHeader.startsWith("Bearer "))
			? refreshHeader.substring(7)
			: null;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		return path.equals("/api/login") || path.equals("/api/signup/session") || path.equals("/api/logout") || path.equals("/api/signup/nickname");
	}
}

