package com.cinefinder.global.config;

import com.cinefinder.chat.service.KafkaService;
import com.cinefinder.movie.service.MovieService;
import com.cinefinder.theater.service.BrandService;
import com.cinefinder.theater.service.TheaterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitConfig implements ApplicationRunner {

    private final BrandService brandService;
    private final TheaterService theaterService;
    private final MovieService movieService;
    private final KafkaService kafkaService;

    @Async
    @Override
    public void run(ApplicationArguments args) {
        brandService.initializeBrands();

        try {
            theaterService.getTheaterInfosAfterSync();
        } catch (Exception e) {
            log.info("({})의 이유로 다음 작업으로 넘어갑니다.", e.getMessage());
        }

        movieService.fetchMultiplexMovieDetails();

        kafkaService.subscribeToAllChatTopics();
    }
}
