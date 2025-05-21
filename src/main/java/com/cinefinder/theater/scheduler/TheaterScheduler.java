package com.cinefinder.theater.scheduler;

import com.cinefinder.theater.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TheaterScheduler {
    private final TheaterService theaterService;

    @Scheduled(cron = "0 0 11 * * *")
    public void fetchTheaterInfoScheduler() {
        theaterService.getTheaterInfosAfterSync();
    }
}
