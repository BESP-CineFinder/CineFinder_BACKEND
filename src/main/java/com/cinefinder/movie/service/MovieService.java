package com.cinefinder.movie.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.Movie;
import com.cinefinder.movie.data.model.BoxOffice;
import com.cinefinder.movie.data.model.MovieDetails;
import com.cinefinder.movie.data.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {
    @Value("${api.kmdb.request-url}")
    private String kmdbRequestUrl;

    @Value("${api.kmdb.service-key}")
    private String kmdbServiceKey;

    @Value("${movie.cgv.name}")
    private String cgvBrandName;

    @Value("${movie.mega.name}")
    private String megaBrandName;

    @Value("${movie.lotte.name}")
    private String lotteBrandName;

    private final RedissonClient redissonClient;
    private final MovieDbSyncService movieDbSyncService;
    private final MovieRepository movieRepository;
    private final MovieDetailService movieDetailService;
    private final BoxOfficeService boxOfficeService;

    public List<BoxOffice> fetchDailyBoxOfficeInfo() {
        return boxOfficeService.getDailyBoxOfficeInfo();
    }

    public MovieDetails fetchMovieDetails(String title) {
        return movieDetailService.getMovieDetails(title);
    }

    public void fetchMultiplexMovieDetails() {
        RLock lock = redissonClient.getLock("multiplex-movie-sync-lock");
        log.info("🔒[영화 상세정보 초기화] 락 시도중...");
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(10, 300, TimeUnit.SECONDS);

            if (!isLocked) {
                log.info("🔒[영화 상세정보 초기화] 다른 서버에서 영화 상세정보를 이미 갱신하고 있어서 초기화를 스킵합니다.");
                return;
            }

            log.info("🔒[영화 상세정보 초기화] 락 획득 성공!");
            movieDbSyncService.fetchMultiplexMovieDetails();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("동기화 중단됨", e);
        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }
    }

    @Transactional(readOnly = true)
    public Movie fetchMovieByBrandMovieCode(String brandName, String movieCode) {
        if (brandName.equals(cgvBrandName)) {
            return movieRepository.findByCgvCode(movieCode);
        } else if (brandName.equals(lotteBrandName)) {
            return movieRepository.findByLotteCinemaCode(movieCode);
        } else if (brandName.equals(megaBrandName)) {
            return movieRepository.findByMegaBoxCode(movieCode);
        } else {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<Movie> getFavoriteMovieList(List<Long> movieIdList) {
        try {
            return movieRepository.findByMovieIdList(movieIdList);
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "좋아요 등록한 영화 ID 목록 조회 중 오류 발생");
        }
    }
}