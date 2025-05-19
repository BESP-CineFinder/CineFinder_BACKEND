package com.cinefinder.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.user.data.entity.Role;
import com.cinefinder.user.data.entity.User;
import com.cinefinder.user.data.repository.UserRepository;
import com.cinefinder.user.data.request.UserSignUpRequestDto;
import com.cinefinder.user.data.response.UserInfoResponseDto;
import com.cinefinder.user.data.response.UserSessionResponseDto;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

	private final UserRepository userRepository;

	@Transactional
	public void completeSignup(UserSignUpRequestDto request) {
		if (userRepository.findByKakaoSub(request.getKakaoSub()).isPresent()) {
			throw new IllegalStateException("User already exists");
		}

		User user = User.builder()
			.kakaoSub(request.getKakaoSub())
			.kakaoEmail(request.getKakaoEmail())
			.nickname(request.getNickname())
			.role(Role.USER) // 기본 사용자 권한
			.build();
		userRepository.save(user);
	}

	public UserSessionResponseDto getSessionDataToDto(HttpSession session) {
		String kakaoSub = (String)session.getAttribute("kakaoSub");
		String kakaoEmail = (String)session.getAttribute("kakaoEmail");

		if (kakaoSub == null || kakaoEmail == null) {
			throw new CustomException(ApiStatus._BAD_REQUEST, "Session data is missing");
		}

		return UserSessionResponseDto.builder()
			.kakaoSub(kakaoSub)
			.kakaoEmail(kakaoEmail)
			.build();
	}

	public UserInfoResponseDto getUserInfo(User user) {
		return UserInfoResponseDto.builder()
			.nickname(user.getNickname())
			.build();
	}

	public UserInfoResponseDto getUserInfoById(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ApiStatus._NOT_FOUND, "User not found"));
		return UserInfoResponseDto.builder()
			.nickname(user.getNickname())
			.build();
	}
}