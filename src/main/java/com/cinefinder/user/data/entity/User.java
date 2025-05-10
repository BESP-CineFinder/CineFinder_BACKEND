package com.cinefinder.user.data.entity;

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
public class User {

	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String nickname;

	private String googleSub;

	private String googleEmail;

	@Enumerated(EnumType.STRING)
	private Role role;

	@Builder
	public User(String nickname, String googleSub, String googleEmail, Role role) {
		this.nickname = nickname;
		this.googleSub = googleSub;
		this.googleEmail = googleEmail;
		this.role = role;
	}

	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}
}
