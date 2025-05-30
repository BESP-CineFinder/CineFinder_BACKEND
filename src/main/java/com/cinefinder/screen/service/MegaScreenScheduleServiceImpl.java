package com.cinefinder.screen.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.Movie;
import com.cinefinder.movie.mapper.MovieMapper;
import com.cinefinder.movie.service.MovieService;
import com.cinefinder.screen.data.dto.CinemaScheduleApiResponseDto;
import com.cinefinder.theater.mapper.TheaterMapper;
import com.cinefinder.brand.service.BrandService;
import com.cinefinder.theater.service.TheaterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MegaScreenScheduleServiceImpl implements ScreenScheduleService{

    @Getter
    @Value("${movie.mega.name}")
    private String brandName;

    private final BrandService brandService;
    private final TheaterService theaterService;
    private final MovieService movieService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MEGABOX_URL = "https://m.megabox.co.kr/on/oh/ohb/SimpleBooking/selectBokdList.do";

    @Override
    public List<CinemaScheduleApiResponseDto> getTheaterSchedule(String playYMD, List<String> movieIds, List<String> theaterIds) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("playDe", playYMD);
        for (int i = 0; i < theaterIds.size(); i++) {
            requestBody.put("brchNo" + (i + 1), theaterIds.get(i));
        }
        for (int i = 0; i < movieIds.size(); i++) {
            requestBody.put("movieNo" + (i + 1), movieIds.get(i));
        }
        if (movieIds.isEmpty()) {
            requestBody.put("movieNo1", "");
        }
        requestBody.put("menuId", "M-RE-MO-02");
        requestBody.put("imgSizeDiv", "IMG_TYPE_7");
        requestBody.put("flag", "VIEW_STEP3");
        requestBody.put("sellChnlCd", "MOBILEWEB");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(MEGABOX_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                JsonNode root = objectMapper.readTree(responseBody);
                return parseScheduleResponse(root);
            }
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "API TYPE=MEGABOX MOVIE SCHEDULE, 영화 ID=" + movieIds + ", 극장 ID=" + theaterIds + " 오류=" + response.getBody());

        } catch (Exception e) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "API TYPE=MEGABOX MOVIE SCHEDULE, 영화 ID=" + movieIds + ", 극장 ID=" + theaterIds + " 오류=" + e.getMessage());
        }
    }

    private List<CinemaScheduleApiResponseDto> parseScheduleResponse(JsonNode root) {
        JsonNode scheduleList = root.get("scheduleList");
        List<CinemaScheduleApiResponseDto> result = new ArrayList<>();
        for (JsonNode item : scheduleList) {
            String movieCode = item.path("rpstMovieNo").asText();
            Movie movie = movieService.fetchMovieByBrandMovieCode(brandName, movieCode);
            if (movie == null) {
                log.warn("{}에서 찾을 수 없는 영화 정보가 있습니다. MovieCode: {}, MovieName: {}", brandName, movieCode, item.path("MovieNm").asText());
                continue;
            }
            try {
                int remainSeat = item.path("restSeatCnt").asInt();
                int capacitySeat = item.path("totSeatCnt").asInt();
                if (remainSeat > capacitySeat || remainSeat <= 0 || capacitySeat <= 0) {
                    log.warn("{}에서 예약이 불가능한 상영 일정 정보가 있습니다. MovieCode: {}, MovieName: {}, RemainingSeat: {}, CapacitySeat: {}", brandName, movieCode, item.path("MovieNmKor").asText(), remainSeat, capacitySeat);
                    continue;
                }
            } catch (Exception e) {
                log.warn("{}에서 예약이 불가능한 상영 일정 정보가 있습니다. MovieCode: {}, MovieName: {}, RemainingSeat: {}, CapacitySeat: {}", brandName, movieCode, item.path("MovieNmKor").asText(), item.path("restSeatCnt").asText(), item.path("totSeatCnt").asText());
                continue;
            }

            CinemaScheduleApiResponseDto dto = new CinemaScheduleApiResponseDto(
                    brandService.getBrandInfo(brandName),
                    TheaterMapper.toSimplifiedTheaterDto(theaterService.getTheaterInfo(brandName, item.path("brchNo").asText())),
                    MovieMapper.toSimplifiedMovieDto(movieService.fetchMovieByBrandMovieCode(brandName, item.path("rpstMovieNo").asText())),
                    item.path("rpstMovieNo").asText(),
                    item.path("theabKindCd").asText(),
                    item.path("playKindNm").asText(),
                    item.path("theabNo").asText(),
                    item.path("theabExpoNm").asText(),
                    item.path("playDe").asText(),
                    item.path("playStartTime").asText(),
                    item.path("playEndTime").asText(),
                    item.path("moviePlayTime").asText(),
                    item.path("restSeatCnt").asText(),
                    item.path("totSeatCnt").asText()
            );
            result.add(dto);
        }
        return result;
    }
}
