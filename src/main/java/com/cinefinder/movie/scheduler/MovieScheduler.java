package com.cinefinder.movie.scheduler;

import com.cinefinder.movie.service.BoxOfficeService;
import com.cinefinder.movie.service.MovieDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieScheduler {
    private final BoxOfficeService boxOfficeService;
    private final MovieDetailService movieDetailService;

    @Scheduled(cron = "0 0 11 * * *")
    public void fetchMovieInfoScheduler() throws Exception {
        try {
            boxOfficeService.fetchDailyBoxOfficeInfo();
        } catch (Exception e) {
            // TODO: 박스오피스 정보 스케줄러 실패 시 예외 처리
            throw new RuntimeException("박스오피스 정보 패치 중 오류 발생", e);
        }

        try {
            movieDetailService.fetchMultiflexMovieDetailList();
        } catch (Exception e) {
            // TODO: 멀티플렉스 3사 영화 상세정보 스케줄러 실패 시 예외 처리
            throw new RuntimeException("멀티플렉스 3사 영화 상세정보 패치 중 오류 발생", e);
        }
    }
}
