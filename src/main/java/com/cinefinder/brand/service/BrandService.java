package com.cinefinder.brand.service;

import com.cinefinder.brand.data.entity.Brand;
import com.cinefinder.brand.data.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandService {

    @Value("${movie.cgv.name}")
    private String cgvBrandName;

    @Value("${movie.mega.name}")
    private String megaBrandName;

    @Value("${movie.lotte.name}")
    private String lotteBrandName;

    private final BrandRepository brandRepository;

    public Brand getBrandInfo(String brandName) {
        return brandRepository.findByName(brandName);
    }

    public void initializeBrands() {
        if (brandRepository.count() == 0) {
            List<Brand> initialBrands = List.of(
                    Brand.builder().name(cgvBrandName).build(),
                    Brand.builder().name(lotteBrandName).build(),
                    Brand.builder().name(megaBrandName).build()
            );
            try {
                brandRepository.saveAll(initialBrands);
                log.info("🔖[브랜드 초기화] 초기 브랜드를 저장했습니다.");
            } catch (ConstraintViolationException | DataIntegrityViolationException e) {
                log.warn("🔖[브랜드 초기화] 다른 서버에서 브랜드를 이미 처리하고 있어서 초기화를 스킵합니다.");
            }
        }
    }
}
