package com.cinefinder.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Value("${rabbitmq.websocket_endpoint}")
	private String WEBSOCKET_ENDPOINT;

	@Value("${rabbitmq.user}")
	private String login;

	@Value("${rabbitmq.password}")
	private String passcode;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableStompBrokerRelay("/topic", "/queue")
			.setRelayHost("localhost")
			.setRelayPort(61613)
			.setSystemLogin(login)
			.setSystemPasscode(passcode)
			.setClientLogin(login)
			.setClientPasscode(passcode);
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint(WEBSOCKET_ENDPOINT).setAllowedOrigins("*").withSockJS();
	}
}
