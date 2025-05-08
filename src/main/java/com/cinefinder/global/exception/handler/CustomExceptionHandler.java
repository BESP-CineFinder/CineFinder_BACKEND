package com.cinefinder.global.exception.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class CustomExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<BaseResponse<Object>> handleApiException(CustomException ex) {
		Class<?> origin = extractOriginClass(ex);
		log.error("API 예외 발생: {} from {}", ex.getMessage(), origin.getSimpleName());
		return ResponseMapper.errorOf(ex.getStatus(), origin);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<Object>> handleUnknownException(Exception ex) {
		Class<?> origin = extractOriginClass(ex);
		log.error("알 수 없는 예외 발생: {}", ex.getMessage(), ex);
		return ResponseMapper.errorOf(ApiStatus._INTERNAL_SERVER_ERROR, origin);
	}

	private Class<?> extractOriginClass(Exception ex) {
		for (StackTraceElement element : ex.getStackTrace()) {
			try {
				if (element.getClassName().startsWith("com.sangchu")) {
					return Class.forName(element.getClassName());
				}
			} catch (ClassNotFoundException ignored) {}
		}
		return CustomExceptionHandler.class;
	}
}


