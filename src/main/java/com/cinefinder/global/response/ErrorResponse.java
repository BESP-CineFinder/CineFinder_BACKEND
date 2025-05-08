package com.cinefinder.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답 엔티티")
public class ErrorResponse<T> extends BaseResponse<T> {

	@Schema(description = "성공 여부", example = "false")
	protected final boolean isSuccess = false;

	@Schema(description = "응답 코드", example = "400")
	protected final int code;

	@Schema(description = "응답 메시지", example = "잘못된 요청입니다.")
	protected final String message;

	@Schema(description = "응답 데이터", example = "null")
	protected final T payload = null;

	public ErrorResponse(int code, String message, Class<?> handleClass) {
		super(false, code, message, null, handleClass);
		this.code = code;
		this.message = message;
	}


}
