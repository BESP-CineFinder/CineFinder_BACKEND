package com.cinefinder.global.oauth2.service;

import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.cinefinder.global.oauth2.entity.CustomOAuth2User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2Service extends DefaultOAuth2UserService {

	// @Override
	// public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
	// 	OAuth2User oAuth2User = super.loadUser(userRequest);
	// 	String sub = oAuth2User.getAttribute("sub");
	// 	String email = oAuth2User.getAttribute("email");
	//
	// 	return new CustomOAuth2User(sub, email, oAuth2User.getAttributes());
	// }

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

		Map<String, Object> attributes = oAuth2User.getAttributes();

		Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
		// Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
		String kakaoSub = String.valueOf(attributes.get("id"));

		String email = (String) kakaoAccount.get("email");

		return new CustomOAuth2User(kakaoSub, email, oAuth2User.getAttributes());
	}
}
