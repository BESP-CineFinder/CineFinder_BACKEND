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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CgvTheaterCrawlerServiceImpl implements TheaterCrawlerService {

    @Getter
    @Value("${movie.cgv.name}")
    private String brandName;

    @Value("${movie.cgv.main-url}")
    private String mainUrl;

    @Value("${movie.cgv.theater-default-endpoint}")
    private String theaterDefaultEndpoint;

    @Value("${movie.cgv.theater-detail-endpoint}")
    private String theaterDetailEndpoint;

    private final BrandRepository brandRepository;
    private final TheaterRepository theaterRepository;

    private static final String USER_AGENT = "Mozilla/5.0";

    @Override
    public List<Theater> getCrawlData() {
        List<Theater> theaters = new ArrayList<>();
        Document mainDoc;
        try {
            mainDoc = Jsoup.connect(mainUrl+theaterDefaultEndpoint).userAgent(USER_AGENT).get();
        } catch (IOException e) {
            // TODO: JSoup 연결 실패 시 예외 처리
            throw new RuntimeException(e);
        }
        Elements areaLinks = mainDoc.select("div.cgv_choice.linktype.area a");

        for (Element areaLink : areaLinks) {
            try {
                theaters.addAll(crawlArea(areaLink));
            } catch (IOException e) {
                // TODO: DB에 저장된 영화관 정보가 없을 경우 예외 처리
                throw new RuntimeException(e);
            }
        }
        return theaters;
    }

    @Override
    @Transactional
    public void syncRecentTheater(List<Theater> theaters) {
        Set<String> existingCodes = getExistingTheaterCodes();
        Set<String> newCodes = extractTheaterCodes(theaters);

        if (existingCodes.isEmpty()) {
            log.info("✅ CGV 영화관 정보가 없습니다. 새로 저장합니다.");
            theaterRepository.saveAll(theaters);
            return;
        }

        if (existingCodes.equals(newCodes)) {
            log.info("✅ CGV 영화관 정보가 이미 최신입니다.");
            return;
        }

        log.info("⁉️ CGV 영화관 정보 변경 확인! 업데이트 시작...");
        theaterRepository.deleteByBrandName(brandName);
        theaterRepository.saveAll(theaters);
        log.info("✅ CGV 영화관 정보 업데이트 완료!");
    }

    private List<Theater> crawlArea(Element areaLink) throws IOException {
        List<Theater> theaters = new ArrayList<>();
        String areaUrl = mainUrl + "/TheaterV4/" + areaLink.attr("href");
        Document areaDoc = Jsoup.connect(areaUrl).userAgent(USER_AGENT).get();

        Element theaterDiv = areaDoc.selectFirst("div.cgv_choice.area");
        if (theaterDiv == null) return theaters;

        for (Element theaterLink : theaterDiv.select("a")) {
            theaters.addAll(processTheaterLink(theaterLink));
        }
        return theaters;
    }

    private List<Theater> processTheaterLink(Element theaterLink) throws IOException {
        List<Theater> theaters = new ArrayList<>();
        String href = theaterLink.attr("href");
        String code = getQueryParam(href, "tc");
        if (code == null) return theaters;

        String name = extractTheaterName(theaterLink).replace("CGV", "").trim();
        LatLng latLng = fetchTheaterCoordinates(code);

        if (latLng != null) {
            Theater theater = Theater.builder()
                    .brand(brandRepository.findByName(brandName))
                    .code(code)
                    .name(name)
                    .latitude(BigDecimal.valueOf(latLng.lat()))
                    .longitude(BigDecimal.valueOf(latLng.lng()))
                    .build();

            theaters.add(theater);
            log.info("CGV 영화관 정보 가져오기 완료: {} - {}", name, code);
        }
        return theaters;
    }

    private String extractTheaterName(Element theaterLink) {
        return Optional.ofNullable(theaterLink.selectFirst("strong"))
                .map(Element::text)
                .map(text -> text.split("\\(")[0].trim())
                .orElse("이름없음");
    }

    private LatLng fetchTheaterCoordinates(String code) throws IOException {
        Document detailDoc = Jsoup.connect(mainUrl + theaterDetailEndpoint + code).userAgent(USER_AGENT).get();

        for (Element script : detailDoc.select("script")) {
            if (script.html().contains("moveNaverMap")) {
                Matcher matcher = Pattern.compile(
                        "lat=\\\"\\s*\\+\\s*\\\"([\\d.]+)\\\".*?lng=\\\"\\s*\\+\\s*\\\"([\\d.]+)\\\""
                ).matcher(script.html());

                if (matcher.find()) {
                    double lat = Double.parseDouble(matcher.group(1));
                    double lng = Double.parseDouble(matcher.group(2));
                    return new LatLng(lat, lng);
                }
            }
        }
        return null;
    }

    private String getQueryParam(String href, String key) {
        return Arrays.stream(href.split("&"))
                .map(p -> p.split("="))
                .filter(arr -> arr.length == 2 && arr[0].endsWith(key))
                .map(arr -> arr[1])
                .findFirst()
                .orElse(null);
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
