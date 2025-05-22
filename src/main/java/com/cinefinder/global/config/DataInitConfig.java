package com.cinefinder.global.config;

import com.cinefinder.chat.service.KafkaService;
import com.cinefinder.movie.service.MovieService;
import com.cinefinder.theater.service.BrandService;
import com.cinefinder.theater.service.TheaterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitConfig {

    private final BrandService brandService;
    private final TheaterService theaterService;
    private final MovieService movieService;
    private final KafkaService kafkaService;

    @PostConstruct
    public void init() {
        brandService.initializeBrands();

        theaterService.getTheaterInfosAfterSync();

        movieService.fetchMultiplexMovieDetails();

        kafkaService.subscribeToAllChatTopics();
    }
}
