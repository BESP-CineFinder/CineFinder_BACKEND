package com.cinefinder.theater.data.init;

import com.cinefinder.theater.data.Brand;
import com.cinefinder.theater.data.repository.BrandRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BrandDataInitializer {

    private final BrandRepository brandRepository;

    public BrandDataInitializer(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @PostConstruct
    public void init() {
        if (brandRepository.count() == 0) {
            List<Brand> initialBrands = List.of(
                    new Brand("CGV"),
                    new Brand("롯데시네마"),
                    new Brand("메가박스")
            );
            brandRepository.saveAll(initialBrands);
        }
    }
}