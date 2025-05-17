package com.cinefinder.movie.scheduler;

import com.cinefinder.movie.service.BoxOfficeService;
import com.cinefinder.movie.service.MovieDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MovieScheduler {
    private final BoxOfficeService boxOfficeService;
    private final MovieDetailService movieDetailService;

    @Scheduled(cron = "0 0 11 * * *")
    public void fetchMovieInfoScheduler() throws Exception {
        try {
            boxOfficeService.fetchDailyBoxOfficeInfo();
        } catch (Exception e) {
            log.error("‼️ 박스오피스 정보 패치 중 오류 발생", e);
        }

        try {
            movieDetailService.fetchMultiplexMovieDetails();
        } catch (Exception e) {
            log.error("‼️ 멀티플렉스 3사 영화 상세정보 패치 중 오류 발생", e);
        }
    }
}
