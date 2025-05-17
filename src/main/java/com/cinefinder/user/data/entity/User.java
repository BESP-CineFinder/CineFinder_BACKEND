package com.cinefinder.user.data.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User implements Serializable {
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String nickname;

	private String kakaoSub;

	private String kakaoEmail;

	@Enumerated(EnumType.STRING)
	private Role role;

	@Builder
	public User(String nickname, String kakaoSub, String kakaoEmail, Role role) {
		this.nickname = nickname;
		this.kakaoSub = kakaoSub;
		this.kakaoEmail = kakaoEmail;
		this.role = role;
	}

	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}
}
