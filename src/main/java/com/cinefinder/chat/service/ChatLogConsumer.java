package com.cinefinder.chat.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cinefinder.chat.data.entity.ChatMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatLogConsumer {

    private final ChatLogElasticService chatLogElasticService;
    private final KafkaConsumer<String, ChatMessage> kafkaConsumer;
    private final RedisSessionService redisSessionService;

    @Scheduled(fixedDelay = 5000)
    public void consumeAndBulkInsert() {
        ConsumerRecords<String, ChatMessage> records = kafkaConsumer.poll(Duration.ofMillis(3000));
        if (!records.isEmpty()) {
            try {
                // 파티션 별로 처리
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, ChatMessage>> partitionRecords = records.records(partition);
                    List<ChatMessage> messages = new ArrayList<>();

                    for (ConsumerRecord<String, ChatMessage> record : partitionRecords) {
                        messages.add(record.value());
                    }

                    // topic 이름 == Elasticsearch index 이름
                    String indexName = partition.topic();

                    // 개별 파티션별 저장 시도
                    try {
                        log.info("✅ Saving messages to index {} : {}", indexName, messages.size());
                        chatLogElasticService.saveBulk(indexName, messages); // 이 saveBulk는 indexName을 받는 형태여야 함
                        String movieId = extractMovieIdFromTopic(indexName); // topic이 "chat-{movieId}"라면 movieId만 추출
                        redisSessionService.clearCachedMessages(movieId);
                        kafkaConsumer.commitSync(Collections.singletonMap(partition,
                            new OffsetAndMetadata(partitionRecords.get(partitionRecords.size() - 1).offset() + 1)));
                    } catch (Exception e) {
                        log.error("Failed to save messages for index {}. Will seek back", indexName, e);
                        long firstOffset = partitionRecords.get(0).offset();
                        kafkaConsumer.seek(partition, firstOffset); // 실패 시 재처리 위해 seek
                    }
                }
            } catch (Exception e) {
                log.error("Unexpected error during partitioned processing", e);
            }
        }
    }

    private String extractMovieIdFromTopic(String topic) {
        return topic.replace("chat-", "");
    }
}