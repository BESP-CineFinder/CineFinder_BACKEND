package com.cinefinder.global.util.AOP;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
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

		if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
			throw new CustomException(ApiStatus._AUTHENTICATION_FAIL, "로그인이 필요한 서비스입니다.");
		}
		return joinPoint.proceed();
	}
}
