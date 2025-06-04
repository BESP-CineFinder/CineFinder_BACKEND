package com.cinefinder.recommend.scheduler;

import com.cinefinder.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class RecommendScheduler {
    private final RecommendService recommendService;

    @Scheduled(cron = "0 */5 * * * *")
    public void putRecommendMovieListScheduler() throws Exception {
        try {
            recommendService.putRecommendMovieListIfAbsent();
        } catch (Exception e) {
            log.error("‼️ 추천 영화목록 스케줄러 실행 중 오류 발생", e);
        }
    }
}