package com.cinefinder.movie.scheduler;

import com.cinefinder.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieScheduler {
    private final MovieService movieService;

    @Scheduled(cron = "0 0 0 * * *")
    public void fetchBoxOfficeScheduler() throws Exception {
        movieService.fetchDailyBoxOfficeInfo();
    }
}
