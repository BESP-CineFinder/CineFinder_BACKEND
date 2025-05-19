package com.cinefinder.chat.service;

import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.chat.data.repository.ChatLogEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLogElasticService {

    private final ElasticsearchOperations elasticsearchOperations;

    public void save(ChatMessage dto) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);

        ChatLogEntity entity = ChatLogEntity.builder()
                .id(UUID.randomUUID().toString())  // 고유한 UUID 생성
                .senderId(dto.getSenderId())
                .message(dto.getMessage())
                .timestamp(timestamp)
                .build();

        // 인덱스명 동적 생성: chat-log-{movieId}
        String indexName = "chat-log-" + dto.getMovieId(); // movieId가 id에 들어 있다고 가정

        // 인덱스 존재 여부 확인 후 생성
        IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(indexName));
        if (!indexOps.exists()) {
            indexOps.create();  // 설정이 필요하면 매핑도 지정 가능
        }

        // 저장
        elasticsearchOperations.save(entity, IndexCoordinates.of(indexName));
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