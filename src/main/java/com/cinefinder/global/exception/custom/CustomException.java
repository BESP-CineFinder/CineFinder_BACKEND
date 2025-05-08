package com.cinefinder.global.exception.custom;

import com.cinefinder.global.util.statuscode.ApiStatus;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
	private final ApiStatus status;

	public CustomException(ApiStatus status) {
		super(status.getMessage());
		this.status = status;
	}

	public CustomException(ApiStatus status, String message) {
		super(status.getMessage() + ":" + message);
		this.status = status;
	}

}
