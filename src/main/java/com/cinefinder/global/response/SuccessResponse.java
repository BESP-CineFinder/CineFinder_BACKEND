package com.cinefinder.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "성공 응답 엔티티")
public class SuccessResponse<T> extends BaseResponse<T> {

	@Schema(description = "성공 여부", example = "true")
	protected final boolean isSuccess = true;

	@Schema(description = "응답 코드", example = "200")
	protected final int code;

	@Schema(description = "응답 메시지", example = "성공적으로 처리되었습니다.")
	protected final String message;

	@Schema(description = "응답 데이터")
	protected final T payload;

	public SuccessResponse(int code, String message, T payload, Class<?> handleClass) {
		super(true, code, message, payload, handleClass);
		this.code = code;
		this.message = message;
		this.payload = payload;
	}


}
