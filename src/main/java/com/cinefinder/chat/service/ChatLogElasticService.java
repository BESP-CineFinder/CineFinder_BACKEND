package com.cinefinder.chat.service;

import com.cinefinder.chat.data.dto.reponse.ChatResponseDto;
import com.cinefinder.chat.data.dto.request.ChatRequestDto;
import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.chat.data.entity.ChatLogEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatLogElasticService {

    private final ElasticsearchOperations elasticsearchOperations;

	public void saveBulk(String movieId, List<ChatMessage> messages) {
		if (messages.isEmpty()) {
			return;
		}

		IndexCoordinates indexCoordinates = IndexCoordinates.of(movieId);
		IndexOperations indexOps = elasticsearchOperations.indexOps(indexCoordinates);

		if (!indexOps.exists()) {
			indexOps.create();
			indexOps.putMapping(indexOps.createMapping(ChatMessage.class));
		}

		List<IndexQuery> indexQueries = messages.stream().map(msg -> {
			IndexQuery query = new IndexQuery();
			query.setId(UUID.randomUUID().toString());
			query.setObject(msg);
			return query;
		}).toList();

		elasticsearchOperations.bulkIndex(indexQueries, indexCoordinates);
	}


	// ✅ 3. Elasticsearch에서 메시지 조회
	public List<ChatLogEntity> findByMovieId(String movieId) {
		Query query = new StringQuery("{\"match_all\": {}}");
		SearchHits<ChatLogEntity> searchHits = elasticsearchOperations.search(query, ChatLogEntity.class, IndexCoordinates.of("chat-log-" + movieId));
		return searchHits.getSearchHits().stream()
				.map(hit -> hit.getContent())
				.toList();
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

	public List<ChatResponseDto> getMessages(ChatRequestDto dto) {
		List<ChatResponseDto> a = new ArrayList<>();
		return a;
	}
}