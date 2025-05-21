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
import org.springframework.stereotype.Service;

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

		IndexCoordinates indexCoordinates = IndexCoordinates.of("chat-log-" + movieId);
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
}