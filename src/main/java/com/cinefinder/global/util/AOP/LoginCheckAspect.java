package com.cinefinder.global.util.AOP;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.cinefinder.global.oauth2.entity.CustomUserDetails;

@Aspect
@Component
public class LoginCheckAspect {

	@Pointcut("@annotation(com.cinefinder.global.util.annotation.LoginRequired)")
	public void loginRequiredMethods() {}

	@Around("loginRequiredMethods()")
	public Object checkLogin(ProceedingJoinPoint joinPoint) throws Throwable {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		// TODO : 업보
		if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
			throw new IllegalStateException("로그인이 필요합니다.");
		}
		return joinPoint.proceed();
	}
}
