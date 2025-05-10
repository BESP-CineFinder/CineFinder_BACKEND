package com.cinefinder.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinefinder.user.data.entity.Role;
import com.cinefinder.user.data.entity.User;
import com.cinefinder.user.data.repository.UserRepository;

import lombok.RequiredArgsConstructor;


@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	public void completeSignup(String googleSub, String nickname) {
		if (userRepository.findByGoogleSub(googleSub).isPresent()) {
			throw new IllegalStateException("User already exists");
		}

		User user = User.builder()
			.googleSub(googleSub)
			.nickname(nickname)
			.role(Role.USER) // 기본 사용자 권한
			.build();
		userRepository.save(user);
	}
}