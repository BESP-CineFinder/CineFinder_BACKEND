package com.cinefinder.theater.service;

import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.data.repository.BrandRepository;
import com.cinefinder.theater.data.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

    private final BrandRepository brandRepository;
    private final TheaterRepository theaterRepository;

    private static final String BASE_URL = "https://m.cgv.co.kr/WebApp/TheaterV4/";
    private static final String MAIN_URL = BASE_URL + "Default.aspx";
    private static final String DETAIL_URL = BASE_URL + "TheaterDetail.aspx?tc=";
    private static final String USER_AGENT = "Mozilla/5.0";

    @Override
    public List<Theater> getCrawlData() {
        List<Theater> theaters = new ArrayList<>();
        Document mainDoc;
        try {
            mainDoc = Jsoup.connect(MAIN_URL).userAgent(USER_AGENT).get();
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
    public void syncTheaterChanges() {
        List<Theater> newTheaters = getCrawlData();
        Set<String> existingCodes = getExistingTheaterCodes();
        Set<String> newCodes = extractTheaterCodes(newTheaters);

        if (existingCodes.isEmpty()) {
            log.info("✅ CGV 영화관 정보가 없습니다. 새로 저장합니다.");
            theaterRepository.saveAll(newTheaters);
            return;
        }

        if (existingCodes.equals(newCodes)) {
            log.info("✅ CGV 영화관 정보가 이미 최신입니다.");
            return;
        }

        log.info("✅ CGV 영화관 정보 업데이트 시작");
        theaterRepository.deleteAll();
        theaterRepository.saveAll(newTheaters);
    }

    private List<Theater> crawlArea(Element areaLink) throws IOException {
        List<Theater> theaters = new ArrayList<>();
        String areaUrl = BASE_URL + areaLink.attr("href");
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

        String name = extractTheaterName(theaterLink);
        LatLng latLng = fetchTheaterCoordinates(code);

        if (latLng != null) {
            Theater theater = Theater.builder()
                    .brand(brandRepository.findByName("CGV"))
                    .code(code)
                    .name(name)
                    .latitude(BigDecimal.valueOf(latLng.lat()))
                    .longitude(BigDecimal.valueOf(latLng.lng()))
                    .build();

            theaters.add(theater);
            log.info("Theater 정보 가져오기 완료: {} - {}", name, code);
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
        Document detailDoc = Jsoup.connect(DETAIL_URL + code).userAgent(USER_AGENT).get();

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
        return theaterRepository.findAll().stream()
                .map(Theater::getCode)
                .collect(Collectors.toSet());
    }

    private Set<String> extractTheaterCodes(List<Theater> theaters) {
        return theaters.stream()
                .map(Theater::getCode)
                .collect(Collectors.toSet());
    }
}
