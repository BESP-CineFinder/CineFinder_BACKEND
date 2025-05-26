package com.cinefinder.chat.service;

import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Slf4j
public class KafkaService {
	private final AdminClient adminClient;
	private final KafkaConsumer<String, ChatMessage> kafkaConsumer;
	private final ChatLogElasticService chatLogElasticService;
	private static final int MAX_RETRIES = 5;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private final KafkaTemplate<String, ChatMessage> kafkaTemplate;

	public void createTopicIfNotExists(String movieId) {
		String topicName = "chat-log-" + movieId;

		try {
			Set<String> existingTopics = adminClient.listTopics().names().get();

			if (!existingTopics.contains(topicName)) {
				NewTopic topic = TopicBuilder.name(topicName)
					.partitions(2)
					.replicas(2)
					.build();
				adminClient.createTopics(Collections.singletonList(topic)).all().get();
				log.info("✅ Kafka topic {} created successfully", topicName);
			}
		} catch (Exception e) {
			throw new CustomException(ApiStatus._CREATE_TOPIC_FAIL);
		}
	}

	public void subscribeToAllChatTopics() {
		try {
			Set<String> topicNames = adminClient.listTopics().names().get();
			List<String> chatTopics = topicNames.stream()
				.filter(t -> t.startsWith("chat-log-"))
				.toList();

			if (!chatTopics.isEmpty()) {
				kafkaConsumer.subscribe(chatTopics);
				log.info("Subscribed to topics: {}", chatTopics);
			} else {
				log.warn("No chat topics found to subscribe.");
			}
		} catch (Exception e) {
			log.error("Failed to subscribe to chat topics", e);
		}
	}

	public void sendMessageWithRetry(ChatMessage message, int attempt) {
		CompletableFuture<SendResult<String, ChatMessage>> future = kafkaTemplate.send(
			"chat-log-" + message.getMovieId(), null, message);

		future.whenComplete((result, throwable) -> {
			if (throwable == null) {
				log.info("Kafka message sent successfully on attempt {} : {}", attempt, message);
			} else {
				// 실패
				log.error("Failed to send Kafka message on attempt {}: {}", attempt, message);
				if (attempt < MAX_RETRIES) {
					long delay = (long)Math.pow(2, attempt); // 지수 백오프
					log.error("Retrying after {} seconds, attempt {}", delay, (attempt + 1));
					scheduler.schedule(() -> sendMessageWithRetry(message, attempt + 1), delay, TimeUnit.SECONDS);
				} else {
					log.error("Max retry attempts reached. Could not send message: {}", message);
					chatLogElasticService.saveBulk("chat-log-" + message.getMovieId(), Collections.singletonList(message));
				}
			}
		});
	}

	public void sendMessage(ChatMessage message) {
		sendMessageWithRetry(message, 1);
	}
}
