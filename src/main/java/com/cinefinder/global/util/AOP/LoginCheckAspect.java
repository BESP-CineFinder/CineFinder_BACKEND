package com.cinefinder.global.util.AOP;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.cinefinder.user.data.entity.User;

@Aspect
@Component
public class LoginCheckAspect {

	@Pointcut("@annotation(com.cinefinder.global.util.annotation.LoginRequired)")
	public void loginRequiredMethods() {}

	@Around("loginRequiredMethods()")
	public Object checkLogin(ProceedingJoinPoint joinPoint) throws Throwable {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
			throw new IllegalStateException("로그인이 필요합니다.");
		}

		User user = (User) authentication.getPrincipal();

		Object[] args = joinPoint.getArgs();
		Object[] newArgs = new Object[args.length + 1];
		System.arraycopy(args, 0, newArgs, 0, args.length);
		newArgs[args.length] = user;

		return joinPoint.proceed(newArgs);  // 여기서만 proceed() 호출 가능
	}
}
