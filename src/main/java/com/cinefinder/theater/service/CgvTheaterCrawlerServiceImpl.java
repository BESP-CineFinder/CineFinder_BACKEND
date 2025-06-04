package com.cinefinder.theater.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.theater.data.entity.Theater;
import com.cinefinder.brand.data.repository.BrandRepository;
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

    private static final String USER_AGENT = "Mozilla/5.0";

    @Override
    public List<Theater> getCrawlData() {
        List<Theater> theaters = new ArrayList<>();
        Document mainDoc;
        try {
            mainDoc = Jsoup.connect(mainUrl+theaterDefaultEndpoint).userAgent(USER_AGENT).get();
        } catch (IOException e) {
            throw new CustomException(ApiStatus._JSOUP_CONNECT_FAIL, e.getMessage());
        }
        Elements areaLinks = mainDoc.select("div.cgv_choice.linktype.area a");

        for (Element areaLink : areaLinks) {
            try {
                theaters.addAll(crawlArea(areaLink));
            } catch (IOException e) {
                throw new CustomException(ApiStatus._THEATER_NOT_FOUND, e.getMessage());
            }
        }
        return theaters;
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

        String name = extractTheaterName(theaterLink).replace(brandName, "").trim();
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
}
