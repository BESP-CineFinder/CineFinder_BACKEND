package com.cinefinder.chat.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.cinefinder.chat.data.entity.ChatMessage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.admin.AdminClient;
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

    @Scheduled(fixedDelay = 5000)
    public void consumeAndBulkInsert() {
        ConsumerRecords<String, ChatMessage> records = kafkaConsumer.poll(Duration.ofMillis(3000));
        log.info("Polled {} records", records.count());
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
                        log.info("start offset : {}", partitionRecords.get(0).offset());
                        chatLogElasticService.saveBulk(indexName, messages); // 이 saveBulk는 indexName을 받는 형태여야 함
                        kafkaConsumer.commitSync(Collections.singletonMap(partition,
                            new OffsetAndMetadata(partitionRecords.get(partitionRecords.size() - 1).offset() + 1)));
                        log.info("end offset : {}", partitionRecords.get(0).offset());
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
}