package com.cinefinder.chat.service;

import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.chat.data.repository.ChatLogEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLogElasticService {

    private final ElasticsearchOperations elasticsearchOperations;

    private final Map<String, List<ChatMessage>> buffer = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 10000) // 10초마다 flush
    public void flushToElasticsearch() {
        buffer.forEach((movieId, messages) -> {
            if (messages.isEmpty()) return;

            String indexName = "chat-log-" + movieId;
            IndexCoordinates index = IndexCoordinates.of(indexName);
            IndexOperations indexOps = elasticsearchOperations.indexOps(index);
            if (!indexOps.exists()) indexOps.create();
            List<IndexQuery> bulkQueries = messages.stream()
                .map(message -> {
                    IndexQuery query = new IndexQuery();
                    query.setId(UUID.randomUUID().toString());
                    query.setObject(message);
                    return query;
                })
                .toList();

            elasticsearchOperations.bulkIndex(bulkQueries, index);
            messages.clear();
        });
    }

    public void add(ChatMessage dto) {
        String movieId = dto.getMovieId();
        buffer.putIfAbsent(movieId, Collections.synchronizedList(new ArrayList<>()));

        buffer.get(movieId).add(new ChatMessage(
            dto.getSenderId(),
            dto.getMessage(),
            dto.getMovieId(),
            dto.getCreatedAt()
        ));
    }

    public List<ChatLogEntity> findAll(String movieId) {
        // 인덱스 이름 구성
        String indexName = "chat-log-" + movieId;

        // match_all 쿼리로 전체 조회
        Query query = new StringQuery("{ \"match_all\": {} }");

        // Elasticsearch에서 검색 수행
        SearchHits<ChatLogEntity> searchHits = elasticsearchOperations.search(
                query,
                ChatLogEntity.class,
                IndexCoordinates.of(indexName)
        );

        // 결과 수집
        List<ChatLogEntity> result = new ArrayList<>();
        searchHits.forEach(hit -> result.add(hit.getContent()));
        return result;
    }
}