package com.cinefinder.chat.data.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatLogRepository extends ElasticsearchRepository<ChatLogEntity, String> {

    // 예시: 최근 메시지 조회 (원하면 추가 구현 가능)
    List<ChatLogEntity> findByMessageContaining(String keyword);
}