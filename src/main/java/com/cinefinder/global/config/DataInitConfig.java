package com.cinefinder.global.config;

import com.cinefinder.movie.service.MovieDetailService;
import com.cinefinder.theater.data.Brand;
import com.cinefinder.theater.data.repository.BrandRepository;
import com.cinefinder.theater.service.TheaterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

@Slf4j
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
            try {
                brandRepository.saveAll(initialBrands);
                log.info("✅[브랜드 초기화] 초기 브랜드를 저장했습니다.");
            } catch (ConstraintViolationException | DataIntegrityViolationException e) {
                log.warn("⚠️[브랜드 초기화] 다른 서버에서 브랜드를 이미 처리하고 있어서 초기화를 스킵합니다.");
            }
        }

        theaterService.getTheaterInfosAfterSync();

        movieDetailService.fetchMultiplexMovieDetails();
    }
}
