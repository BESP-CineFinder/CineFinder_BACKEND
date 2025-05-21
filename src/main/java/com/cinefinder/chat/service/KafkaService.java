package com.cinefinder.chat.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class KafkaService {
    private final AdminClient adminClient;

    public void createTopicIfNotExists(String movieId) {
        String topicName = "chat-log-" + movieId;

        try {
            Set<String> existingTopics = adminClient.listTopics().names().get();

            if (!existingTopics.contains(topicName)) {
                NewTopic topic = TopicBuilder.name(topicName)
                    .partitions(3)
                    .replicas(2)
                    .build();
                adminClient.createTopics(Collections.singletonList(topic)).all().get();
                log.info("✅ Kafka topic {} created successfully", topicName);
            }
        } catch (Exception e) {
            throw new CustomException(ApiStatus._CREATE_TOPIC_FAIL);
        }
    }

}
