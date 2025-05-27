package com.cinefinder.chat.service;

import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.chat.data.entity.ChatLogEntity;
import com.cinefinder.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

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
}