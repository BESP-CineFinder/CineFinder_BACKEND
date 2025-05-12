package com.cinefinder.theater.data.init;

import com.cinefinder.theater.data.Brand;
import com.cinefinder.theater.data.repository.BrandRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BrandDataInitializer {

    @Value("${movie.cgv.name}")
    private String cgvBrandName;

    @Value("${movie.mega.name}")
    private String megaBrandName;

    @Value("${movie.lotte.name}")
    private String lotteBrandName;

    private final BrandRepository brandRepository;

    public BrandDataInitializer(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

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
    }
}