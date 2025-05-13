package com.cinefinder.screen.service;

import com.cinefinder.screen.data.dto.ScreenScheduleResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CgvScreenScheduleService implements ScreenScheduleService {

    ObjectMapper objectMapper = new ObjectMapper();
    OkHttpClient client = new OkHttpClient();

    private String getRequiredCookies(String movieParam, String theaterParam) {

        String refererUrl = String.format(
                "https://m.cgv.co.kr/WebApp/Reservation/QuickResult.aspx?mc=&mgc=%s&tc=%s&ymd=&rt=MOVIE&fst=&fet=&fsrc=&fmac=",
                movieParam, theaterParam
        );

        String dataImgPath = "MovieImg.aspx?MovieIdx=" + movieParam.split("\\|")[0];
        String encodedPath = URLEncoder.encode("\"" + dataImgPath + "\"", StandardCharsets.UTF_8);

        String reservationUrl = "https://m.cgv.co.kr/WebApp/Reservation/" + encodedPath;

        Request request = new Request.Builder()
                .url(reservationUrl)
                .get()
                .header("Referer", refererUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("응답 실패: " + response.code());
            }

            Headers headers = response.headers();
            return headers.values("Set-Cookie").stream()
                .map(cookie -> cookie.split(";", 2)[0])
                .collect(Collectors.joining("; "));
        } catch (IOException e) {
            // TODO: 호출 실패 시 예외 처리
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ScreenScheduleResponseDto> getTheaterSchedule(String playYMD, List<String> movieIds, List<String> theaterIds) throws IOException {

        String movieParam = String.join("|", movieIds);
        String theaterParam = String.join("|", theaterIds);

        String requiredCookies = getRequiredCookies(theaterParam, theaterParam);

        String requestBodyJson = """
        {
          "strRequestType": "COMPARE",
          "strUserID": "",
          "strMovieGroupCd": "%s",
          "strMovieTypeCd": "",
          "strPlayYMD": "%s",
          "strTheaterCd": "%s",
          "strScreenTypeCd": "",
          "strRankType": "MOVIE"
        }
        """.formatted(
                movieParam,
                playYMD,
                theaterParam
        );

        MediaType mediaType = MediaType.parse("application/json");
        okhttp3.RequestBody body = RequestBody.create(mediaType, requestBodyJson);
        Request request = new Request.Builder()
                .url("https://m.cgv.co.kr/WebAPP/Reservation/Common/ajaxTheaterScheduleList.aspx/GetTheaterScheduleList")
                .method("POST", body)
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("Accept-Language", "ko,en-US;q=0.9,en;q=0.8,ko-KR;q=0.7")
                .addHeader("Origin", "https://m.cgv.co.kr")
                .addHeader("Content-Type", "application/json")
                .addHeader("Cookie", requiredCookies)
                .build();
        Response response = client.newCall(request).execute();
        String responseBody = response.body() != null ? response.body().string() : "";

        JsonNode root = objectMapper.readTree(responseBody);
        String innerJsonStr = root.get("d").asText();
        JsonNode scheduleList = objectMapper.readTree(innerJsonStr).get("ResultSchedule").get("ScheduleList");

        List<ScreenScheduleResponseDto> result = new ArrayList<>();
        for (JsonNode item : scheduleList) {
            ScreenScheduleResponseDto dto = new ScreenScheduleResponseDto(
                    "CGV",
                    item.path("TheaterCd").asText(),
                    item.path("TheaterNm").asText(),
                    item.path("MovieCd").asText(),
                    item.path("MovieNmKor").asText(),
                    item.path("MovieNmEng").asText(),
                    item.path("MovieRatingCd").asText(),
                    item.path("MovieRatingNm").asText(),
                    item.path("ScreenCd").asText(),
                    item.path("ScreenNm").asText(),
                    item.path("PlayYmd").asText(),
                    item.path("PlayStartTm").asText(),
                    item.path("PlayEndTm").asText(),
                    item.path("RunningTime").asText(),
                    item.path("SeatRemainCnt").asText(),
                    item.path("SeatCapacity").asText(),
                    item.path("AllowSaleYn").asText(),
                    item.path("PlatformCd").asText(),
                    item.path("PlatformNm").asText(),
                    item.path("MovieAttrCd").asText(),
                    item.path("MovieAttrNm").asText(),
                    item.path("PosterImageUrl").asText()
            );
            result.add(dto);
        }


        return result;
    }

    //    public String getTheaterScheduleWithRestTemplate(String playYMD, List<String> movieIds, List<String> theaterIds) {
//        String movieParam = String.join("|", movieIds);
//        String theaterParam = String.join("|", theaterIds);
//
//        String refererUrl = String.format(
//                "https://m.cgv.co.kr/WebApp/Reservation/QuickResult.aspx?mc=&mgc=%s&tc=%s&ymd=&rt=MOVIE&fst=&fet=&fsrc=&fmac=",
//                movieParam, theaterParam
//        );
//
//        // 1. 먼저 쿠키 얻기 위한 URL 호출
//        String dataImgPath = "MovieImg.aspx?MovieIdx=" + movieIds.get(0); // 예시
//        String encodedPath = URLEncoder.encode("\"" + dataImgPath + "\"", StandardCharsets.UTF_8);
//        String reservationUrl = "https://m.cgv.co.kr/WebApp/Reservation/" + encodedPath;
//        log.info(reservationUrl);
//
//        HttpHeaders headers1 = new HttpHeaders();
//        headers1.set("Referer", refererUrl);
//        headers1.set("User-Agent", "Mozilla/5.0");
//
//        HttpEntity<Void> request1 = new HttpEntity<>(headers1);
//        ResponseEntity<String> response1 = restTemplate.exchange(reservationUrl, HttpMethod.GET, request1, String.class);
//
//        List<String> cookies = response1.getHeaders().get(HttpHeaders.SET_COOKIE);
//
//        // 2. 얻은 쿠키를 두 번째 요청에 사용
//        HttpHeaders headers2 = new HttpHeaders();
//        headers2.set("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G973N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36");
//        headers2.setContentType(MediaType.APPLICATION_JSON);
//        headers2.setAccept(List.of(MediaType.APPLICATION_JSON));
//        headers2.set("Accept", "application/json, text/javascript, */*; q=0.01");
//        headers2.set("Accept-Language", "ko,en-US;q=0.9,en;q=0.8,ko-KR;q=0.7");
//        headers2.set("Origin", "https://m.cgv.co.kr");
////        headers2.set("Referer", refererUrl);
//        String cookieValues = cookies.stream()
//                .map(cookie -> cookie.split(";", 2)[0])  // "key=value"만 추출
//                .collect(Collectors.joining("; "));
//        headers2.set(HttpHeaders.COOKIE, cookieValues);
//
//        String requestBodyJson = """
//        {
//          "strRequestType": "COMPARE",
//          "strUserID": "",
//          "strMovieGroupCd": "%s",
//          "strMovieTypeCd": "",
//          "strPlayYMD": "%s",
//          "strTheaterCd": "%s",
//          "strScreenTypeCd": "",
//          "strRankType": "MOVIE"
//        }
//        """.formatted(
//                movieParam,
//                playYMD,
//                theaterParam
//        );
//
//        HttpEntity<String> request2 = new HttpEntity<>(requestBodyJson, headers2);
//        log.info(request2.getBody());
//        log.info(String.valueOf(request2.getHeaders()));
//
//        ResponseEntity<String> response2 = restTemplate.postForEntity(
//                "https://m.cgv.co.kr/WebApp/Reservation/Common/ajaxTheaterScheduleList.aspx/GetTheaterScheduleList",
//                request2,
//                String.class
//        );
//
//        return response2.getBody();
//    }
}
