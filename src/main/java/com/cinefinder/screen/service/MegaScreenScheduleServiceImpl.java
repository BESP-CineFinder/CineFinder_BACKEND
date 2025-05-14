package com.cinefinder.screen.service;

import com.cinefinder.screen.data.dto.ScreenScheduleResponseDto;
import com.cinefinder.theater.data.dto.SimplifiedTheaterDto;
import com.cinefinder.theater.data.repository.BrandRepository;
import com.cinefinder.theater.data.repository.TheaterRepository;
import com.cinefinder.theater.mapper.TheaterMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final BrandRepository brandRepository;
    private final TheaterRepository theaterRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MEGABOX_URL = "https://m.megabox.co.kr/on/oh/ohb/SimpleBooking/selectBokdList.do";

    @Override
    public List<ScreenScheduleResponseDto> getTheaterSchedule(String playYMD, List<String> movieIds, List<String> theaterIds) {

        // POST 요청 바디 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("playDe", playYMD);
        requestBody.put("brchNo1", theaterIds.get(0));
        if (theaterIds.size() > 1) requestBody.put("brchNo2", theaterIds.get(1));
        requestBody.put("movieNo1", movieIds.get(0));
        if (movieIds.size() > 1) requestBody.put("movieNo2", movieIds.get(1));
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
                JsonNode scheduleList = objectMapper.readTree(responseBody).get("scheduleList");

                return parseScheduleListFromMegaboxJson(scheduleList);
            } else {
                throw new RuntimeException("메가박스 API 호출 실패: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("메가박스 API 요청 중 오류 발생", e);
        }
    }

    // Dummy parser (응답 구조 분석 후 수정 필요)
    private List<ScreenScheduleResponseDto> parseScheduleListFromMegaboxJson(JsonNode scheduleList) {
        List<ScreenScheduleResponseDto> result = new ArrayList<>();
        for (JsonNode item : scheduleList) {
            ScreenScheduleResponseDto dto = new ScreenScheduleResponseDto(
                    brandRepository.findByName("메가박스"),
                    TheaterMapper.toSimplifiedTheaterDto(theaterRepository.findByBrandNameAndCode("메가박스", item.path("brchNo").asText())),
                    item.path("rpstMovieNo").asText(),
                    item.path("movieNm").asText(),
                    item.path("movieEngNm").asText(),
                    item.path("theabNo").asText(),
                    item.path("theabExpoNm").asText(),
                    item.path("playDe").asText(),
                    item.path("playStartTime").asText(),
                    item.path("playEndTime").asText(),
                    item.path("moviePlayTime").asText(),
                    item.path("restSeatCnt").asText(),
                    item.path("totSeatCnt").asText(),
                    item.path("theabKindCd").asText(),
                    item.path("playKindNm").asText()
            );
            result.add(dto);
        }
        return result;
    }
}
