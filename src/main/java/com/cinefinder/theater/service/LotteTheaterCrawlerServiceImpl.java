package com.cinefinder.theater.service;

import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.data.repository.BrandRepository;
import com.cinefinder.theater.data.repository.TheaterRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("롯데시네마TheaterCrawler")
@RequiredArgsConstructor
public class LotteTheaterCrawlerServiceImpl implements TheaterCrawlerService {

    private static final String LOTTE_API_URL = "https://www.lottecinema.co.kr/LCWS/Cinema/CinemaData.aspx";
    private final BrandRepository brandRepository;
    private final TheaterRepository theaterRepository;
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
                LOTTE_API_URL,
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
                        .brand(brandRepository.findByName("롯데시네마"))
                        .code(code)
                        .name(name)
                        .latitude(latitude)
                        .longitude(longitude)
                        .build();

                theaters.add(theater);
                log.info("롯데시네마 영화관 정보 가져오기 완료: {} - {}", name, code);
            }
        } catch (Exception e) {
            throw new RuntimeException("롯데시네마 영화관 정보 파싱 중 오류 발생", e);
        }

        return theaters.stream()
                .collect(Collectors.toMap(Theater::getCode, theater -> theater, (existing, replacement) -> existing))
                .values()
                .stream()
                .toList();
    }

    @Override
    @Transactional
    public void syncRecentTheater(List<Theater> theaters) {
        Set<String> existingCodes = getExistingTheaterCodes();
        Set<String> newCodes = extractTheaterCodes(theaters);

        if (existingCodes.isEmpty()) {
            log.info("✅ 롯데시네마 영화관 정보가 없습니다. 새로 저장합니다.");
            theaterRepository.saveAll(theaters);
            return;
        }

        if (existingCodes.equals(newCodes)) {
            log.info("✅ 롯데시네마 영화관 정보가 이미 최신입니다.");
            return;
        }

        log.info("⁉️ 롯데시네마 영화관 정보 변경 확인! 업데이트 시작...");
        theaterRepository.deleteByBrandName("롯데시네마");
        theaterRepository.saveAll(theaters);
        log.info("✅ 롯데시네마 영화관 정보 업데이트 완료!");
    }

    private Set<String> getExistingTheaterCodes() {
        return theaterRepository.findByBrandName("롯데시네마").stream()
                .map(Theater::getCode)
                .collect(Collectors.toSet());
    }

    private Set<String> extractTheaterCodes(List<Theater> theaters) {
        return theaters.stream()
                .map(Theater::getCode)
                .collect(Collectors.toSet());
    }
}
