package com.cinefinder.global.util.AOP;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import com.cinefinder.global.util.jwt.JwtUtil;
import com.cinefinder.global.util.service.TokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LogoutAspect {

	private final JwtUtil jwtUtil;
	private final TokenService tokenService;

	@Around("@annotation(com.cinefinder.global.util.annotation.LogoutRequired)")
	public Object handleLogout(ProceedingJoinPoint joinPoint) throws Throwable {
		// 1. 로그인 여부 확인
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new RuntimeException("로그인된 사용자가 아닙니다.");
		}

		// 2. 현재 요청 객체 가져오기
		ServletRequestAttributes sra = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
		if (sra == null) {
			throw new IllegalStateException("Request context is not available");
		}
		HttpServletRequest request = sra.getRequest();
		HttpServletResponse response = sra.getResponse();
		if (response == null) {
			throw new IllegalStateException("Response object is not available");
		}

		// 3. 토큰 유효성 검사 및 사용자 정보 추출
		String token = jwtUtil.resolveToken(request);
		log.info("Logout token: {}", token);

		if (token == null || !jwtUtil.validateToken(token)) {
			throw new RuntimeException("유효하지 않은 토큰입니다.");
		}

		String username = jwtUtil.getUsernameFromToken(token);
		long remainTime = jwtUtil.getExpirationMillis(token);

		// 4. 토큰 블랙리스트 처리 및 리프레시 토큰 삭제
		tokenService.blacklistAccessToken(token, remainTime);
		tokenService.deleteRefreshToken(username);
		log.info("Logout 완료: {}", username);

		// ✅ 5. Refresh-Token 쿠키 삭제
		ResponseCookie deleteRefreshCookie = ResponseCookie.from("Refresh-Token", "")
			.path("/")
			.httpOnly(true)
			.secure(true)
			.sameSite("None")
			.maxAge(0)
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString());

		// ✅ 6. 세션 무효화 및 JSESSIONID 삭제
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		Cookie deleteSessionCookie = new Cookie("JSESSIONID", null);
		deleteSessionCookie.setPath("/");
		deleteSessionCookie.setMaxAge(0);
		response.addCookie(deleteSessionCookie);

		// 7. 컨트롤러 비즈니스 로직 실행
		return joinPoint.proceed();
	}
}

