package com.cinefinder.screen.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.screen.data.dto.ScreenScheduleResponseDto;
import com.cinefinder.theater.data.repository.BrandRepository;
import com.cinefinder.theater.data.repository.TheaterRepository;
import com.cinefinder.theater.mapper.TheaterMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteScreenScheduleServiceImpl implements ScreenScheduleService {

    @Value("${movie.lotte.name}")
    private String brandName;

    private final BrandRepository brandRepository;
    private final TheaterRepository theaterRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ScreenScheduleResponseDto> getTheaterSchedule(String playYMD, List<String> movieIds, List<String> theaterIds) {
        String formattedDate = playYMD.substring(0, 4) + "-" + playYMD.substring(4, 6) + "-" + playYMD.substring(6, 8);
        List<ScreenScheduleResponseDto> allSchedules = new ArrayList<>();

        for (String theaterId : theaterIds) {
            List<String> targetMovieIds = movieIds.isEmpty() ? List.of("") : movieIds;
            for (String movieId : targetMovieIds) {
                try {
                    List<ScreenScheduleResponseDto> result = requestSchedule(theaterId, movieId, formattedDate);
                    allSchedules.addAll(result);
                } catch (Exception e) {
                    // TODO: 롯데시네마 API 호출 실패 시 예외 처리
                    throw new RuntimeException("롯데시네마 API 호출 실패: theaterId=" + theaterId + ", movieId=" + movieId, e);
                }
            }
        }

        return allSchedules;
    }

    private List<ScreenScheduleResponseDto> requestSchedule(String cinemaId, String movieId, String playDate) throws Exception {
        String url = "https://www.lottecinema.co.kr/LCWS/Ticketing/TicketingData.aspx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String jsonPayload = String.format("""
            {
              "MethodName": "GetPlaySequence",
              "channelType": "HO",
              "osType": "Chrome",
              "osVersion": "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36",
              "playDate": "%s",
              "cinemaID": "1|1|%s",
              "representationMovieCode": "%s"
            }
            """, playDate, cinemaId, movieId);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("ParamList", jsonPayload);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        if (response.getStatusCode().isError()) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "API TYPE=LOTTE MOVIE SCHEDULE, 영화 ID=" + movieId + ", 극장 ID=" + cinemaId + " 오류=" + response.getBody());
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        return parseScheduleResponse(root);
    }

    private List<ScreenScheduleResponseDto> parseScheduleResponse(JsonNode root) {
        List<ScreenScheduleResponseDto> result = new ArrayList<>();

        JsonNode items = root.path("PlaySeqs").path("Items");
        if (!items.isArray()) return result;

        for (JsonNode item : items) {
            String start = item.path("StartTime").asText();  // "12:00"
            String end = item.path("EndTime").asText();      // "13:13"
            long runtimeMinutes = 0;

            if (null !=start && null != end && !start.isEmpty() && !end.isEmpty()) {
                try {
                    int startHour = Integer.parseInt(start.substring(0, 2));
                    int endHour = Integer.parseInt(end.substring(0, 2));

                    String startStr = start;
                    String endStr = end;

                    if (12 < startHour) {
                        startStr = String.format("%02d", startHour-12) + start.substring(2);
                        endStr = String.format("%02d", endHour-12) + end.substring(2);
                    }

                    LocalTime startTime = LocalTime.parse(startStr);
                    LocalTime endTime = LocalTime.parse(endStr);

                    if (endTime.isBefore(startTime)) {
                        endTime = endTime.plusHours(24);
                    }

                    runtimeMinutes = Duration.between(startTime, endTime).toMinutes();
                } catch (Exception e) {
                    log.error("시간 파싱 오류: startTime={}, endTime={}, error={}", start, end, e.getMessage());
                }
            }

            ScreenScheduleResponseDto dto = new ScreenScheduleResponseDto(
                    brandRepository.findByName(brandName),
                    TheaterMapper.toSimplifiedTheaterDto(theaterRepository.findByBrandNameAndCode(brandName, item.path("CinemaID").asText())),
                    item.path("RepresentationMovieCode").asText(),
                    item.path("MovieNameKR").asText(),
                    item.path("MovieNameUS").asText(),
                    item.path("FilmCode").asText(),
                    item.path("FilmNameKR").asText(),
                    item.path("ScreenID").asText(),
                    item.path("ScreenNameKR").asText(""),
                    item.path("StartTime").asText(),
                    item.path("StartTime").asText(),
                    item.path("EndTime").asText(),
                    String.valueOf(runtimeMinutes),
                    item.path("BookingSeatCount").asText(),
                    item.path("TotalSeatCount").asText()
            );

            result.add(dto);
        }

        return result;
    }
}
