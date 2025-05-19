package com.cinefinder.global.config;

import com.cinefinder.movie.service.MovieDetailService;
import com.cinefinder.theater.data.Brand;
import com.cinefinder.theater.data.repository.BrandRepository;
import com.cinefinder.theater.service.TheaterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitConfig {

    @Value("${movie.cgv.name}")
    private String cgvBrandName;

    @Value("${movie.mega.name}")
    private String megaBrandName;

    @Value("${movie.lotte.name}")
    private String lotteBrandName;

    private final BrandRepository brandRepository;
    private final TheaterService theaterService;
    private final MovieDetailService movieDetailService;

    @PostConstruct
    public void init() {
        if (brandRepository.count() == 0) {
            List<Brand> initialBrands = List.of(
                    Brand.builder().name(cgvBrandName).build(),
                    Brand.builder().name(lotteBrandName).build(),
                    Brand.builder().name(megaBrandName).build()
            );
            brandRepository.saveAll(initialBrands);
        }

        theaterService.getTheaterInfosAfterSync();

        movieDetailService.fetchMultiflexMovieDetails();
    }
}
