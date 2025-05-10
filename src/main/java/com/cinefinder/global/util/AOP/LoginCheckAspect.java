package com.cinefinder.global.util.AOP;

import org.aspectj.lang.ProceedingJoinPoint;
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

	@Before("loginRequiredMethods()")
	public void checkLogin(ProceedingJoinPoint joinPoint) throws Throwable {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		// 로그인되지 않은 경우 예외 처리
		if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
			throw new IllegalStateException("로그인이 필요합니다.");
		}

		// 로그인된 사용자 정보 가져오기
		User user = (User) authentication.getPrincipal();

		// 필요시 User 객체를 joinPoint로 넘길 수 있음 (메서드 인자로 전달 가능)
		Object[] args = joinPoint.getArgs();
		Object[] newArgs = new Object[args.length + 1];
		System.arraycopy(args, 0, newArgs, 0, args.length);
		newArgs[args.length] = user;  // User 객체 추가

		// 새로운 인자들로 메서드 실행
		joinPoint.proceed(newArgs);  // 기존 인자들에 User 추가
	}
}
