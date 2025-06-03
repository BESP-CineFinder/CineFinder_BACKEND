package com.cinefinder.recommend.service;

import com.cinefinder.chat.service.ChatLogElasticService;
import com.cinefinder.favorite.service.FavoriteService;
import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.model.BoxOffice;
import com.cinefinder.movie.service.BoxOfficeService;
import com.cinefinder.movie.service.MovieService;
import com.cinefinder.recommend.data.dto.RecommendResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendService {
    private final FavoriteService favoriteService;
    private final MovieService movieService;
    private final BoxOfficeService boxOfficeService;
    private final ChatLogElasticService chatLogElasticService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    private static final String REDIS_KEY = "recommendMovieList";

    public List<RecommendResponseDto> getRecommendMovieList() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            if (redisTemplate.hasKey(REDIS_KEY)) {
                return redisTemplate.opsForHash()
                        .entries(REDIS_KEY)
                        .values()
                        .stream()
                        .map(object -> mapper.convertValue(object, RecommendResponseDto.class))
                        .sorted(Comparator.comparingInt(info -> Integer.parseInt(String.valueOf(info.getRank()))))
                        .limit(10)
                        .toList();
            } else {
                return putRecommendMovieListIfAbsent();
            }
        } catch (Exception e) {
            throw new CustomException(ApiStatus._REDIS_SAVE_FAIL, "추천 영화목록 저장 중 실패");    
        }
    }

    public List<RecommendResponseDto> putRecommendMovieListIfAbsent() {
        RLock lock = redissonClient.getLock("recommend-movie-sync-lock");
        log.info("🔒[추천 영화목록 초기화] 락 시도중...");
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(1, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                log.info("🔒[추천 영화목록 초기화] 다른 서버에서 추천 영화목록을 이미 갱신하고 있어서 초기화를 스킵합니다.");
                return null;
            }

            log.info("🔒[추천 영화목록 초기화] 락 획득 성공!");

            List<Long> movieIds = movieService.findAllMovieIds();
            Map<Long, Double> sentimentMap = chatLogElasticService.getSentimentScores(movieIds);
            List<BoxOffice> boxOfficeList = boxOfficeService.getDailyBoxOfficeInfo();

            Map<Long, Integer> rankMap = new HashMap<>();
            for (BoxOffice boxOffice : boxOfficeList) {
                rankMap.put(boxOffice.getMovieId(), Integer.parseInt(boxOffice.getRank()));
            }

            List<RecommendResponseDto> recommendResponseDtoList = new ArrayList<>();
            for (Map.Entry<Long, Double> entry : sentimentMap.entrySet()) {
                double total = 0;
                Long movieId = entry.getKey();

                total += 3 * favoriteService.countFavoriteMovieList(movieId);
                total += 2 * entry.getValue();
                total += (rankMap.get(movieId) != null) ? 2 - 0.1 * (rankMap.get(movieId) - 1) : 0;

                RecommendResponseDto recommendResponseDto = RecommendResponseDto.builder()
                    .movieId(movieId)
                    .score(total)
                    .movieDetails(movieService.getMovieDetailsByMovieId(movieId))
                    .build();

                recommendResponseDtoList.add(recommendResponseDto);
            }

            redisTemplate.delete(REDIS_KEY);

            PriorityQueue<RecommendResponseDto> topTen = new PriorityQueue<>(Comparator.comparingDouble(RecommendResponseDto::getScore));
            for (RecommendResponseDto dto : recommendResponseDtoList) {
                topTen.offer(dto);
                if (topTen.size() > 10) topTen.poll();
            }

            AtomicInteger rankCounter = new AtomicInteger(1);
            List<RecommendResponseDto> topTenList = topTen.stream()
                .sorted(Comparator.comparing(RecommendResponseDto::getScore).reversed())
                .peek(dto -> dto.updateRank((long) rankCounter.getAndIncrement()))
                .toList();

            for (RecommendResponseDto recommendResponseDto : topTenList) {
                redisTemplate.opsForHash().put(REDIS_KEY, String.valueOf(recommendResponseDto.getRank()), recommendResponseDto);
            }

            return recommendResponseDtoList;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ApiStatus._INTERRUPT_EXCEPTION, "외부의 중단 요청으로 예외 발생: "+ e.getMessage());
        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }
    }
}