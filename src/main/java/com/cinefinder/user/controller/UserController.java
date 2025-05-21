package com.cinefinder.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.annotation.Login;
import com.cinefinder.global.util.annotation.LoginRequired;
import com.cinefinder.global.util.annotation.LogoutRequired;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.user.data.entity.User;
import com.cinefinder.user.data.response.UserInfoResponseDto;
import com.cinefinder.user.data.response.UserSessionResponseDto;
import com.cinefinder.user.service.UserService;
import com.cinefinder.user.data.request.UserSignUpRequestDto;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/signup/session")
	public ResponseEntity<BaseResponse<UserSessionResponseDto>> getSignupSessionInfo(HttpSession session) {
		UserSessionResponseDto userSessionData = userService.getSessionDataToDto(session);
		return ResponseMapper.successOf(
			ApiStatus._OK,
			userSessionData,
			UserController.class
		);
	}

	@PostMapping("/signup/nickname")
	public ResponseEntity<BaseResponse<Object>> completeSignup(@RequestBody UserSignUpRequestDto request) {
		userService.completeSignup(request);
		return ResponseMapper.successOf(
			ApiStatus._CREATED,
			null,
			UserController.class
		);
	}

	@PostMapping("/logout")
	@LogoutRequired
	public ResponseEntity<?> logout() {
		return ResponseEntity.ok("로그아웃 성공");
	}

	@GetMapping("/info")
	@LoginRequired
	public ResponseEntity<BaseResponse<UserInfoResponseDto>> getUserInfo(@Login User user) {
		return ResponseMapper.successOf(
			ApiStatus._OK,
			userService.getUserInfo(user),
			UserController.class
		);
	}

	@GetMapping("/user")
	@LoginRequired
	public ResponseEntity<BaseResponse<UserInfoResponseDto>> getUserNicknameById(@Login User user) {
		return ResponseMapper.successOf(
			ApiStatus._OK,
			userService.getUserInfoById(user.getId()),
			UserController.class
		);
	}
}
