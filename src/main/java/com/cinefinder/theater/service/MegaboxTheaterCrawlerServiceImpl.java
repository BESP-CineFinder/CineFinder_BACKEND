package com.cinefinder.theater.service;

import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.data.repository.BrandRepository;
import com.cinefinder.theater.data.repository.TheaterRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MegaboxTheaterCrawlerServiceImpl implements TheaterCrawlerService {

    @Getter
    @Value("${movie.mega.name}")
    private String brandName;

    @Value("${movie.mega.main-url}")
    private String mainUrl;

    @Value("${movie.mega.theater-default-endpoint}")
    private String theaterDefaultEndpoint;

    @Value("${movie.mega.theater-detail-endpoint}")
    private String theaterDetailEndpoint;

    private final BrandRepository brandRepository;
    private final TheaterRepository theaterRepository;

    private static final String USER_AGENT = "Mozilla/5.0";

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<Theater> getCrawlData() {
        List<Theater> theaters = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(mainUrl + theaterDefaultEndpoint).userAgent(USER_AGENT).get();
            Element placeDiv = doc.selectFirst("div.theater-place");
            if (placeDiv == null) {
                // TODO: 예외처리로 할지 아니면 그냥 리턴할지 고민
                log.warn("❌ 메가박스 사이트에서 영화관 정보 영역 찾을 수 없습니다.(theater-place)");
                return theaters;
            }

            Elements theaterLists = placeDiv.select("div.theater-list");
            for (Element theaterList : theaterLists) {
                for (Element li : theaterList.select("li")) {
                    String code = li.attr("data-brch-no");
                    String name = Optional.ofNullable(li.selectFirst("a"))
                            .map(Element::text)
                            .orElse("이름없음")
                            .split("\\(")[0];

                    LatLng latLng = fetchLatLng(code);
                    if (latLng != null) {
                        Theater theater = Theater.builder()
                                .brand(brandRepository.findByName(brandName))
                                .code(code)
                                .name(name)
                                .latitude(BigDecimal.valueOf(latLng.lat()))
                                .longitude(BigDecimal.valueOf(latLng.lng()))
                                .build();

                        theaters.add(theater);
                        log.info("메가박스 영화관 정보 가져오기 완료: {} - {}", name, code);
                    }
                }
            }

        } catch (IOException e) {
            // TODO: 크롤링 오류 처리
            throw new RuntimeException("메가박스 영화관 목록 크롤링 중 오류 발생", e);
        }

        return theaters;
    }

    @Override
    @Transactional
    public void syncRecentTheater(List<Theater> theaters) {
        Set<String> existingCodes = getExistingTheaterCodes();
        Set<String> newCodes = extractTheaterCodes(theaters);

        if (existingCodes.isEmpty()) {
            log.info("✅ 메가박스 영화관 정보가 없습니다. 새로 저장합니다.");
            theaterRepository.saveAll(theaters);
            return;
        }

        if (existingCodes.equals(newCodes)) {
            log.info("✅ 메가박스 영화관 정보가 이미 최신입니다.");
            return;
        }

        log.info("⁉️ 메가박스 영화관 정보 변경 확인! 업데이트 시작...");
        theaterRepository.deleteByBrandName(brandName);
        theaterRepository.saveAll(theaters);
        log.info("✅ 메가박스 영화관 정보 업데이트 완료!");
    }

    private LatLng fetchLatLng(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", USER_AGENT);

        String json = "{\"brchNo\":\"" + code + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        try {
            String html = restTemplate.postForObject(mainUrl + theaterDetailEndpoint, entity, String.class);
            if (html == null) return null;

            Document doc = Jsoup.parse(html);
            Element mapLink = doc.selectFirst("a[title=새창열림]");
            if (mapLink != null && mapLink.hasAttr("href")) {
                String href = mapLink.attr("href");
                Matcher matcher = Pattern.compile("lng=([\\d.]+)&lat=([\\d.]+)").matcher(href);
                if (matcher.find()) {
                    double lng = Double.parseDouble(matcher.group(1));
                    double lat = Double.parseDouble(matcher.group(2));
                    return new LatLng(lat, lng);
                }
            }
        } catch (Exception e) {
            log.warn("❌ 좌표 추출 실패 (code: {}): {}", code, e.getMessage());
        }

        return null;
    }

    private record LatLng(double lat, double lng) {}

    private Set<String> getExistingTheaterCodes() {
        return theaterRepository.findByBrandName(brandName).stream()
                .map(Theater::getCode)
                .collect(Collectors.toSet());
    }

    private Set<String> extractTheaterCodes(List<Theater> theaters) {
        return theaters.stream()
                .map(Theater::getCode)
                .collect(Collectors.toSet());
    }
}