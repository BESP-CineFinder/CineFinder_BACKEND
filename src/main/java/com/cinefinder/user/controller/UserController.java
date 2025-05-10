package com.cinefinder.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.cinefinder.global.util.annotation.LogoutRequired;
import com.cinefinder.user.service.UserService;
import com.cinefinder.user.data.request.UserSignUpRequestDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PostMapping("/login")
	public ResponseEntity<Void> redirectToLogin() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Location", "/login");  // 리다이렉트할 URL 지정
		return new ResponseEntity<>(headers, HttpStatus.FOUND);  // 302 Found 상태 코드
	}

	@PostMapping("/signup-nickname")
	public ResponseEntity<?> completeSignup(@RequestBody UserSignUpRequestDto request) {
		userService.completeSignup(request.getGoogleSub(), request.getNickname());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/logout")
	@LogoutRequired
	public ResponseEntity<?> logout() {
		return ResponseEntity.ok("로그아웃 성공");
	}
}
