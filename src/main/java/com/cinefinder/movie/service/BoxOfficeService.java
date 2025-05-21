package com.cinefinder.movie.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.Movie;
import com.cinefinder.movie.data.model.BoxOffice;
import com.cinefinder.movie.data.repository.MovieRepository;
import com.cinefinder.movie.mapper.MovieMapper;
import com.cinefinder.movie.util.UtilParse;
import com.cinefinder.movie.util.UtilString;
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

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoxOfficeService {
    @Value("${api.kobis.request-url}")
    private String kobisRequestUrl;

    @Value("${api.kobis.service-key}")
    private String kobisServiceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final RedisTemplate<String, Object> redisTemplate;
    private final MovieRepository movieRepository;

    public List<BoxOffice> getDailyBoxOfficeInfo() {
        ObjectMapper mapper = new ObjectMapper();
        String latestDate = UtilString.getLatestDateString();
        String latestDateRedisKey = "dailyBoxOffice:" + latestDate;

        if (redisTemplate.hasKey(latestDateRedisKey)) {
            log.info("박스오피스 캐시 조회 : {}", latestDateRedisKey);
            return redisTemplate.opsForHash()
                .entries(latestDateRedisKey)
                .entrySet()
                .stream()
                .map(entry -> {
                    String rank = entry.getKey().toString();
                    BoxOffice boxOffice = mapper.convertValue(entry.getValue(), BoxOffice.class);
                    Movie movie = movieRepository.findByMovieKey(boxOffice.getMovieKey())
                        .orElseThrow(() -> new IllegalStateException("‼️ 해당 영화의 상세정보 없음"));
                    boxOffice.updateRank(rank);
                    boxOffice.updateMovieDetails(MovieMapper.toMovieDetails(movie));
                    return boxOffice;
                })
                .sorted(Comparator.comparingInt(info -> Integer.parseInt(info.getRank())))
                .collect(Collectors.toList());
        } else {
            log.info("박스오피스 캐싱 : {}", latestDateRedisKey);
            return fetchDailyBoxOfficeInfo();
        }
    }

    public List<BoxOffice> fetchDailyBoxOfficeInfo() {
        try {
            String latestDate = UtilString.getLatestDateString();
            String latestDateRedisKey = "dailyBoxOffice:" + latestDate;
            String url = String.format(
                kobisRequestUrl + "?key=%s&targetDt=%s",
                kobisServiceKey,
                latestDate
            );

            String response = restTemplate.getForObject(new URI(url), String.class);

            String beforeDate = UtilString.getBeforeDateString();
            String beforeDateRedisKey = "dailyBoxOffice:" + beforeDate;
            redisTemplate.delete(beforeDateRedisKey);

            List<BoxOffice> dailyBoxOfficeList = UtilParse.extractDailyBoxOfficeInfoList(response);
            for (BoxOffice boxOffice : dailyBoxOfficeList) {
                Movie movie = movieRepository.findByMovieKey(boxOffice.getMovieKey())
                    .orElseThrow(() -> new IllegalStateException("‼️ 해당 영화의 상세정보 없음"));
                boxOffice.updateMovieId(movieRepository.findMovieIdByMovieKey(boxOffice.getMovieKey()));
                boxOffice.updateMovieDetails(MovieMapper.toMovieDetails(movie));
                redisTemplate.opsForHash().put(latestDateRedisKey, boxOffice.getRank(), boxOffice);
            }

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