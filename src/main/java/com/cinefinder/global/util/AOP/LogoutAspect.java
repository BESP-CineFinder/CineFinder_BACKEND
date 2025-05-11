package com.cinefinder.global.util.AOP;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.cinefinder.global.util.jwt.JwtUtil;
import com.cinefinder.global.util.service.TokenService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

		// 2. 현재 요청에서 토큰 추출
		ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (sra == null) {
			throw new IllegalStateException("Request context is not available");
		}
		HttpServletRequest request = sra.getRequest();
		String token = jwtUtil.resolveToken(request);
		log.info("Logout token: {}", token);
		// 3. 토큰 유효성 검사 및 사용자 정보 추출
		if (token == null || !jwtUtil.validateToken(token)) {
			throw new RuntimeException("유효하지 않은 토큰입니다.");
		}

		String username = jwtUtil.getUsernameFromToken(token);
		long remainTime = jwtUtil.getExpirationMillis(token);

		// 4. 블랙리스트 및 refresh 삭제 처리
		tokenService.blacklistAccessToken(token, remainTime);
		tokenService.deleteRefreshToken(username);

		log.info("Logout 완료: {}", username);

		// 5. 비즈니스 로직 진행 (컨트롤러 로직 실행)
		return joinPoint.proceed();
	}
}
