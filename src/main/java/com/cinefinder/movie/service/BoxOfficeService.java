package com.cinefinder.movie.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.model.BoxOffice;
import com.cinefinder.movie.util.UtilString;
import com.cinefinder.movie.util.UtilParse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoxOfficeService {
    @Value("${api.kobis.request-url}")
    private String kobisRequestUrl;

    @Value("${api.kobis.service-key}")
    private String kobisServiceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final RedisTemplate<String, Object> redisTemplate;

    public List<BoxOffice> getDailyBoxOfficeInfo() {
        ObjectMapper mapper = new ObjectMapper();

        // 1. 최신일자 계산
        String latestDate = UtilString.getLatestDateString();
        String latestDateRedisKey = "dailyBoxOffice:" + latestDate;
        log.info("🔑 [일간 박스오피스 정보 조회] REDIS 최신일자 키 이름 : {}", latestDateRedisKey);

        // 2. 일간 박스오피스 정보 응답 분기 처리
        if (redisTemplate.hasKey(latestDateRedisKey)) {
            log.info("✅ {} 키 존재 ... 캐시된 데이터 조회", latestDateRedisKey);

            return redisTemplate.opsForHash()
                .entries(latestDateRedisKey)
                .entrySet()
                .stream()
                .map(entry -> {
                    String rank = entry.getKey().toString();
                    BoxOffice boxOffice = mapper.convertValue(entry.getValue(), BoxOffice.class);
                    boxOffice.setRank(rank);
                    return boxOffice;
                })
                .sorted(Comparator.comparingInt(info -> Integer.parseInt(info.getRank())))
                .collect(Collectors.toList());
        } else {
            log.info("✅ {} 키 없음 ... KOBIS API 호출 후 캐싱", latestDateRedisKey);

            return fetchDailyBoxOfficeInfo();
        }
    }

    public List<BoxOffice> fetchDailyBoxOfficeInfo() {
        try {
            // 1. 최신일자 계산
            String latestDate = UtilString.getLatestDateString();
            String latestDateRedisKey = "dailyBoxOffice:" + latestDate;
            log.info("🔑 [일간 박스오피스 정보 저장] REDIS 최신일자 키 이름 : {}", latestDateRedisKey);

            // 2. 요청 URL 생성
            String url = String.format(
                kobisRequestUrl + "?key=%s&targetDt=%s",
                kobisServiceKey,
                latestDate
            );

            // 3. API 요청
            String response = restTemplate.getForObject(new URI(url), String.class);

            // 4. 요청 및 응답 List 생성
            List<BoxOffice> dailyBoxOfficeList = UtilParse.extractDailyBoxOfficeInfoList(response);

            // 5. Redis 직전일자 데이터 삭제
            String beforeDate = UtilString.getBeforeDateString();
            String beforeDateRedisKey = "dailyBoxOffice:" + beforeDate;
            redisTemplate.delete(beforeDateRedisKey);
            if (!redisTemplate.hasKey(beforeDateRedisKey)) log.info("⭕ 직전일자 데이터 삭제 완료");

            // 6. Redis 데이터 저장
            for (BoxOffice boxOffice : dailyBoxOfficeList) {
                redisTemplate.opsForHash().put(latestDateRedisKey, boxOffice.getRank(), boxOffice);
            }
            log.info("⭕ REDIS 저장 완료");

            return dailyBoxOfficeList;
        } catch (URISyntaxException e) {
            throw new CustomException(ApiStatus._INVALID_URI_FORMAT, "일간 박스오피스 정보 저장 중 URI 구분 분석 오류 발생");
        } catch (RestClientException e) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "일간 박스오피스 정보 저장 중 외부 API 호출 오류 발생");
        } catch (Exception e) {
            throw new CustomException(ApiStatus._OPERATION_FAIL, "일간 박스오피스 정보 저장 중 알 수 없는 오류 발생");
        }
    }
}
