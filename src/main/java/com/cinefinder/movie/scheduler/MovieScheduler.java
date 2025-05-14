package com.cinefinder.movie.scheduler;

import com.cinefinder.movie.service.BoxOfficeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieScheduler {
    private final BoxOfficeService boxOfficeService;

    @Scheduled(cron = "0 0 11 * * *")
    public void fetchBoxOfficeScheduler() throws Exception {
        boxOfficeService.fetchDailyBoxOfficeInfo();
    }
}
