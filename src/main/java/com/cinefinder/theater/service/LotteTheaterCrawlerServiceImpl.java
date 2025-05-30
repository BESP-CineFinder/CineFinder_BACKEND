package com.cinefinder.theater.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.theater.data.Theater;
import com.cinefinder.brand.data.repository.BrandRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteTheaterCrawlerServiceImpl implements TheaterCrawlerService {

    @Getter
    @Value("${movie.lotte.name}")
    private String brandName;

    @Value("${movie.lotte.main-url}")
    private String mainUrl;

    @Value("${movie.lotte.theater-default-endpoint}")
    private String theaterDefaultEndpoint;

    private final BrandRepository brandRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Theater> getCrawlData() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "Mozilla/5.0");

        String body = "ParamList=" + URLEncoder.encode("""
            {
                "MethodName":"GetCinemaItems",
                "channelType":"HO",
                "osType":"Chrome",
                "osVersion":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
                "divisionCode":"1",
                "detailDivisionCode":"1",
                "memberOnNo":"0"
            }
            """, StandardCharsets.UTF_8);

        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                mainUrl + theaterDefaultEndpoint,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        List<Theater> theaters = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("Cinemas").path("Items");

            for (JsonNode item : items) {
                String name = item.path("CinemaName").asText().split("\\(")[0].trim();
                String code = item.path("CinemaID").asText();
                BigDecimal latitude = BigDecimal.valueOf(item.path("Latitude").asDouble());
                BigDecimal longitude = BigDecimal.valueOf(item.path("Longitude").asDouble());

                Theater theater = Theater.builder()
                        .brand(brandRepository.findByName(brandName))
                        .code(code)
                        .name(name)
                        .latitude(latitude)
                        .longitude(longitude)
                        .build();

                theaters.add(theater);
            }
        } catch (Exception e) {
            throw new CustomException(ApiStatus._JSON_PARSE_FAIL, e.getMessage());
        }

        return theaters.stream()
                .collect(Collectors.toMap(Theater::getCode, theater -> theater, (existing, replacement) -> existing))
                .values()
                .stream()
                .toList();
    }
}
