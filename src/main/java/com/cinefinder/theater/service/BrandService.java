package com.cinefinder.theater.service;

import com.cinefinder.theater.data.Brand;
import com.cinefinder.theater.data.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public Brand getBrandInfo(String brandName) {
        return brandRepository.findByName(brandName);
    }
}
