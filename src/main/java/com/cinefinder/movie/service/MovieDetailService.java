package com.cinefinder.movie.service;

import com.cinefinder.movie.data.model.MovieDetails;
import com.cinefinder.movie.util.UtilParse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieDetailService {
    @Value("${api.kmdb.request-url}")
    private String kmdbRequestUrl;

    @Value("${api.kmdb.service-key}")
    private String kmdbServiceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final RedisTemplate<String, Object> redisTemplate;

    public MovieDetails getMovieDetails(String movieKey, String title) {
        ObjectMapper mapper = new ObjectMapper();

        String redisKey = "movieDetails:" + movieKey;
        log.info("🔑 [영화 상세정보 조회] REDIS 키 이름 : {}", redisKey);

        if (redisTemplate.hasKey(redisKey)) {
            log.info("✅ {} 키 존재 ... 캐시된 데이터 조회", redisKey);
            
            Object object = redisTemplate.opsForHash().get(redisKey, movieKey);
            return mapper.convertValue(object, MovieDetails.class);
        } else {
            log.info("✅ {} 키 없음 ... KMDB API 호출 후 캐싱", redisKey);
            
            return fetchMovieDetails(movieKey, title);
        }
    }

    public MovieDetails fetchMovieDetails(String movieKey, String title) {
        try {
            String redisKey = "movieDetails:" + movieKey;
            MovieDetails returnMovieDetails = null;
            log.info("🔑 [영화 상세정보 저장] REDIS 키 이름 : {}", redisKey);

            // 1. 요청 URL 생성
            String url = String.format(
                kmdbRequestUrl + "?collection=kmdb_new2&detail=Y&ServiceKey=%s&title=%s&sort=repRlsDate,1&listCount=1",
                kmdbServiceKey,
                URLEncoder.encode(title, StandardCharsets.UTF_8)
            );

            // 2. API 요청
            String response = restTemplate.getForObject(new URI(url), String.class);

            // 3. 저장 List 생성
            List<MovieDetails> movieDetailsList = UtilParse.extractMovieDetailsList(response);

            // 4. 응답 결과가 2개 이상이라면
            if (movieDetailsList.size() >= 2) {
                log.warn("❌ API 1개의 요청 파라미터에 응답 결과가 2개 이상");

                for (MovieDetails movieDetails : movieDetailsList) log.warn("{}", movieDetails.getTitle());
                throw new IllegalArgumentException("영화 상세정보 데이터 캐싱 전 프로세스 중단");
            }

            // 5. Redis 데이터 저장 및 만료일자 설정
            for (MovieDetails movieDetails : movieDetailsList) {
                log.info("⭕ 영화 상세정보 데이터 캐싱 성공");

                redisTemplate.opsForHash().put(redisKey, movieKey, movieDetails);
                redisTemplate.expire(redisKey, 1, TimeUnit.DAYS);
                returnMovieDetails = movieDetails;
            }

            return returnMovieDetails;
        } catch (Exception e) {
            // TODO: API 1개의 요청 파라미터에 응답 결과가 2개 이상일 경우 예외 처리
            throw new RuntimeException("영화 상세정보 저장 중 오류 발생", e);
        }
    }
}