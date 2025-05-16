package com.cinefinder.chat.service;

import com.cinefinder.chat.data.dto.ChatMessageDto;
import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class KafkaService {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ChatMessageDto> fetchLogsByMovieId(String movieId) {
        String topicName = "chat-log-" + movieId;

        if (!topicExists(topicName)) {
            throw new CustomException(ApiStatus._NOT_EXISTENT_TOPIC);
        }

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "fetch-logs-" + UUID.randomUUID()); // 매번 다른 그룹으로
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        List<ChatMessageDto> result = new ArrayList<>();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topicName));

            int emptyPollCount = 0;
            final int maxEmptyPolls = 15; // 3번 연속 empty면 종료

            while (emptyPollCount < maxEmptyPolls) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(300));

                if (records.isEmpty()) {
                    emptyPollCount++;
                } else {
                    emptyPollCount = 0; // 다시 초기화
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            ChatMessageDto message = objectMapper.readValue(record.value(), ChatMessageDto.class);
                            String timestamp = String.valueOf(record.timestamp());
                            message.setTimestamp(timestamp);
                            result.add(message);
                        } catch (Exception e) {
                            throw new CustomException(ApiStatus._MESSAGE_PARSE_FAIL);
                        }
                    }
                }
            }
        }

        return result;
    }

    private boolean topicExists(String topicName) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(configs)) {
            return adminClient.listTopics().names().get().contains(topicName);
        } catch (Exception e) {
            return false;
        }
    }

    public void createTopicIfNotExists(String movieId) {
        String topicName = "chat-log-" + movieId; // 영화 ID 기반 토픽
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(configs)) {
            // 존재 여부만 빠르게 확인
            try {
                adminClient.describeTopics(Collections.singletonList(topicName)).allTopicNames().get();
            } catch (Exception e) {
                NewTopic topic = TopicBuilder.name(topicName)
                        .partitions(3)
                        .replicas(2)
                        .build();
                adminClient.createTopics(Collections.singletonList(topic)).all().get();
            }
        } catch (Exception e) {
            throw new CustomException(ApiStatus._CREATE_TOPIC_FAIL);
        }
    }
}
