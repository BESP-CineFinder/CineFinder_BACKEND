package com.cinefinder.chat.service;

import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.chat.data.entity.ChatLogEntity;
import com.cinefinder.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.cinefinder.chat.data.dto.ChatSentimentRequestDto;
import com.cinefinder.chat.data.entity.ChatSentiment;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AvgAggregate;

import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;
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
	private final UserService userService;

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

	public List<ChatMessage> getMessagesFromElasticsearch(String movieId, LocalDateTime cursorCreatedAt, int size) {
		String indexName = "chat-log-" + movieId;

		Long cursorCreatedAtEpochMilli = (cursorCreatedAt != null)
				? cursorCreatedAt
				.atZone(ZoneId.of("Asia/Seoul"))
				.withZoneSameInstant(ZoneOffset.UTC)
				.toInstant()
				.toEpochMilli()
				: null;

		String jsonQuery = (cursorCreatedAt != null)
				? String.format("""
				  {
				    "range": {
				      "createdAt": {
				        "lt": "%s"
				      }
				    }
				  }
				""", cursorCreatedAtEpochMilli)
				: "{ \"match_all\": {} }";

		Query query = new StringQuery(jsonQuery);
		query.addSort(Sort.by(Sort.Order.desc("createdAt")));
		query.setPageable(PageRequest.of(0, size));

		SearchHits<ChatLogEntity> searchHits = elasticsearchOperations.search(
				query,
				ChatLogEntity.class,
				IndexCoordinates.of(indexName)
		);

		List<ChatLogEntity> mainHits = searchHits.stream()
				.map(SearchHit::getContent)
				.toList();

		if (mainHits.isEmpty()) return Collections.emptyList();

		String lastCreatedAt = mainHits.getLast().getCreatedAt();
		String tieQuery = String.format("""
				  {
				    "term": {
				      "createdAt": "%s"
				    }
				  }
				""", lastCreatedAt);

		Query tieBreakerQuery = new StringQuery(tieQuery);
		tieBreakerQuery.addSort(Sort.by(Sort.Order.desc("createdAt")));

		SearchHits<ChatLogEntity> tieHits = elasticsearchOperations.search(
				tieBreakerQuery,
				ChatLogEntity.class,
				IndexCoordinates.of(indexName)
		);

		Set<String> seenMessageIds = new HashSet<>();
		List<ChatMessage> results = new ArrayList<>();

		Stream.concat(mainHits.stream(), tieHits.stream().map(SearchHit::getContent))
				.filter(hit -> seenMessageIds.add(hit.getId()))
				.sorted(Comparator.comparing(ChatLogEntity::getCreatedAt).reversed())
				.map(chatLog -> {
					LocalDateTime createdAt = Instant.ofEpochMilli(Long.parseLong(chatLog.getCreatedAt()))
							.atZone(ZoneOffset.UTC)
							.toLocalDateTime();

					return ChatMessage.builder()
							.senderId(chatLog.getSenderId())
							.nickName(userService.getUserInfoById(Long.valueOf(chatLog.getSenderId())).getNickname())
							.message(chatLog.getMessage())
							.createdAt(createdAt)
							.build();
				})
				.forEach(results::add);

		return results;
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