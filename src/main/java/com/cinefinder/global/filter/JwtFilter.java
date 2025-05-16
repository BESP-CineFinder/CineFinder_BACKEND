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

import com.cinefinder.global.oauth2.entity.CustomUserDetails;
import com.cinefinder.global.util.jwt.JwtUtil;
import com.cinefinder.global.util.service.TokenService;
import com.cinefinder.user.data.entity.User;
import com.cinefinder.user.data.repository.UserRepository;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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

		String token = resolveAccessToken(request);

		if (token != null) {
			if (tokenService.isBlacklisted(token)) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is blacklisted");
				return;
			}

			try {
				if (jwtUtil.validateToken(token)) {
					authenticate(token);
				} else {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token.");
					return;
				}
			} catch (ExpiredJwtException e) {
				handleExpiredAccessToken(request, response);
				return;
			}
		} else {
			// ✅ 액세스 토큰 자체가 없는 경우에도 Refresh 토큰으로 인증 시도
			boolean success = handleRefreshOnlyRequest(request, response);
			if (!success) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Login required.");
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(String token) {
		String kakaoSub = jwtUtil.getUsernameFromToken(token);
		Optional<User> userOpt = userRepository.findByKakaoSub(kakaoSub);

		if (userOpt.isPresent()) {
			User user = userOpt.get();
			CustomUserDetails customUserDetails = new CustomUserDetails(user);
			UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
				customUserDetails, null, customUserDetails.getAuthorities()
			);
			SecurityContextHolder.getContext().setAuthentication(auth);
			log.info("Authenticated user: {}", user.getNickname());
		}
	}

	private void handleExpiredAccessToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String refreshToken = resolveRefreshTokenFromCookie(request);
		if (refreshToken == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token missing. Please login again.");
			return;
		}

		try {
			handleRefreshToken(refreshToken, response);
		} catch (RuntimeException e) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token expired. Please login again.");
		}
	}

	private String resolveAccessToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		return (bearerToken != null && bearerToken.startsWith("Bearer "))
			? bearerToken.substring(7)
			: null;
	}

	private String resolveRefreshTokenFromCookie(HttpServletRequest request) {
		if (request.getCookies() == null) return null;

		for (Cookie cookie : request.getCookies()) {
			if ("Refresh-Token".equals(cookie.getName())) {
				String value = cookie.getValue();
				if (value != null && value.startsWith("Bearer ")) {
					return value.substring(7);
				}
				return value;
			}
		}
		return null;
	}

	private boolean handleRefreshOnlyRequest(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = resolveRefreshTokenFromCookie(request);
		if (refreshToken == null) return false;

		try {
			handleRefreshToken(refreshToken, response);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private void handleRefreshToken(String refreshToken, HttpServletResponse response) {
		Map<String, String> newTokens = jwtUtil.refreshToken(refreshToken);
		String newAccessToken = newTokens.get("accessToken");
		String newRefreshToken = newTokens.get("refreshToken");

		// 응답 헤더로 새 토큰 전달
		response.setHeader("Authorization", "Bearer " + newAccessToken);
		response.setHeader("Refresh-Token", "Bearer " + newRefreshToken);

		// 인증 처리까지 진행
		authenticate(newAccessToken);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		return path.equals("/api/login") || path.equals("/api/signup/session")
			|| path.equals("/api/logout") || path.equals("/api/signup/nickname")
			|| path.startsWith("/api/movie") || path.startsWith("/api/theater")
				|| path.startsWith("/api/screen") || path.startsWith("/chat/");
	}
}

