package com.cinefinder.global.util.resolver;

import com.cinefinder.global.util.annotation.Login;
import com.cinefinder.user.data.entity.User;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.stereotype.Component;

@Component  // @Component 어노테이션 추가
public class LoginArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// @Login 어노테이션이 붙은 파라미터인 경우 처리
		return parameter.hasParameterAnnotation(Login.class) && parameter.getParameterType().equals(User.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		// SecurityContextHolder에서 로그인된 사용자 정보를 가져오기
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		// principal이 User 객체인 경우 반환
		if (principal instanceof User) {
			return principal; // User 객체 반환
		}

		// 로그인되지 않은 경우 예외 처리
		throw new IllegalStateException("로그인된 사용자가 아닙니다.");
	}
}