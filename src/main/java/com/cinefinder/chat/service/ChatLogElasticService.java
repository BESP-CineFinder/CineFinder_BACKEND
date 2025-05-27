package com.cinefinder.chat.service;

import com.cinefinder.chat.data.dto.reponse.ChatResponseDto;
import com.cinefinder.chat.data.dto.request.ChatRequestDto;
import com.cinefinder.chat.data.dto.request.ChatSentimentRequestDto;
import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.chat.data.entity.ChatLogEntity;
import com.cinefinder.chat.data.entity.ChatSentiment;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AvgAggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
		String indexName = "chat-log-" + movieId;
		IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
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
		saveScore(movieId, messages);
	}

	private void saveScore(String movieId, List<ChatMessage> messages) {
		ChatSentiment sentiment = getPrefixSumAndCount(messages);

		String indexName = "chat-sentiment-" + movieId;
		IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
		IndexOperations indexOps = elasticsearchOperations.indexOps(indexCoordinates);

		if (!indexOps.exists()) {
			indexOps.create();
			indexOps.putMapping(indexOps.createMapping(ChatSentiment.class));
		}

		// 매번 새 문서 생성 (UUID ID)
		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId(UUID.randomUUID().toString());
		indexQuery.setObject(sentiment);

		elasticsearchOperations.index(indexQuery, indexCoordinates);
	}

	private ChatSentiment getPrefixSumAndCount(List<ChatMessage> messages) {
		RestTemplate restTemplate = new RestTemplate();
		String url = "http://sentiment:8000/predict"; // sentiment 서비스의 URL

		try {
			List<String> messagesText = messages.stream()
				.map(ChatMessage::getFilteredMessage)
				.toList();

			ChatSentimentRequestDto request = new ChatSentimentRequestDto(messagesText);
			String response = restTemplate.postForObject(url, request, String.class);

			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.readValue(response, ChatSentiment.class);

		} catch (Exception e) {
			log.error("Error while calling sentiment service: {}", e.getMessage());
			return new ChatSentiment(0L, 0L);
		}
	}


	public Map<Long, Double> getSentimentScores(List<Long> movieIds) {
		Map<Long, Double> results = new HashMap<>();

		for (Long movieId : movieIds) {
			String indexName = "chat-sentiment-" + movieId;
			IndexCoordinates index = IndexCoordinates.of(indexName);

			NativeQuery query = NativeQuery.builder()
				.withAggregation("avg_score",
					Aggregation.of(b -> b.avg(t -> t.field("score"))))
				.withAggregation("avg_count",
					Aggregation.of(b -> b.avg(t -> t.field("count"))))
				.withMaxResults(0)
				.build();

			SearchHits<ChatSentiment> hits = elasticsearchOperations.search(query, ChatSentiment.class, index);

			AggregationsContainer<?> aggregationsContainer = hits.getAggregations();
			ElasticsearchAggregations aggregations = ((ElasticsearchAggregations)Objects.requireNonNull(aggregationsContainer));
			AvgAggregate avgScoreAgg = Objects.requireNonNull(aggregations.get("avg_score"))
				.aggregation().getAggregate().avg();
			AvgAggregate avgCountAgg = Objects.requireNonNull(aggregations.get("avg_count"))
				.aggregation().getAggregate().avg();

			double avgScore = avgScoreAgg.value();
			double avgCount = avgCountAgg.value();
			double value = avgScore / avgCount;
			double rounded = Math.round(value * 100) / 100.0;

			if (Math.abs(avgCount) < 1e-6) {
				results.put(movieId, 0.0);
				continue;
			}
			results.put(movieId, rounded);
		}
		return results;
	}

	// ✅ 3. Elasticsearch에서 메시지 조회
	public List<ChatLogEntity> findByMovieId(String movieId) {
		Query query = new StringQuery("{\"match_all\": {}}");
		SearchHits<ChatLogEntity> searchHits = elasticsearchOperations.search(query, ChatLogEntity.class,
			IndexCoordinates.of("chat-log-" + movieId));
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

	public void createElasticsearchChatIndex(String movieId) {
		String indexName = "chat-log-" + movieId;

		IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(indexName));
		if (!indexOps.exists()) {
			indexOps.create();

			Map<String, Object> mapping = Map.of(
				"properties", Map.of(
					"id", Map.of("type", "keyword"),
					"senderId", Map.of("type", "keyword"),
					"message", Map.of("type", "text", "analyzer", "standard"),
					"filteredMessage", Map.of("type", "text", "analyzer", "standard"),
					"createdAt", Map.of("type", "date", "format", "strict_date_optional_time||epoch_millis")
				)
			);

			Document mappingDoc = Document.from(mapping);
			indexOps.putMapping(mappingDoc);
		}
	}

	public void createElasticsearchSentimentIndex(String movieId) {
		String indexName = "chat-sentiment-" + movieId;

		IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(indexName));
		if (!indexOps.exists()) {
			indexOps.create();

			Map<String, Object> mapping = Map.of(
				"properties", Map.of(
					"score", Map.of("type", "long"),
					"count", Map.of("type", "long")
				)
			);

			Document mappingDoc = Document.from(mapping);
			indexOps.putMapping(mappingDoc);
		}
	}
}