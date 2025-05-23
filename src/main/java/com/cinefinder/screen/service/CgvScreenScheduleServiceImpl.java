package com.cinefinder.screen.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.Movie;
import com.cinefinder.movie.mapper.MovieMapper;
import com.cinefinder.movie.service.MovieService;
import com.cinefinder.screen.data.dto.CinemaScheduleApiResponseDto;
import com.cinefinder.theater.mapper.TheaterMapper;
import com.cinefinder.theater.service.BrandService;
import com.cinefinder.theater.service.TheaterService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CgvScreenScheduleServiceImpl implements ScreenScheduleService {

    @Getter
    @Value("${movie.cgv.name}")
    private String brandName;

    private final BrandService brandService;
    private final TheaterService theaterService;
    private final MovieService movieService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient();

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
                throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "API TYPE=CGV COOKIE FOR MOVIE SCHEDULE, 영화 ID=" + Arrays.toString(movieParam.split("\\|")) + ", 극장 ID=" + Arrays.toString(theaterParam.split("\\|")) + " 오류=" + response.body());
            }

            Headers headers = response.headers();
            return headers.values("Set-Cookie").stream()
                .map(cookie -> cookie.split(";", 2)[0])
                .collect(Collectors.joining("; "));
        } catch (IOException e) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "API TYPE=CGV COOKIE FOR MOVIE SCHEDULE, 영화 ID=" + Arrays.toString(movieParam.split("\\|")) + ", 극장 ID=" + Arrays.toString(theaterParam.split("\\|")) + " 오류=" + e.getMessage());
        }
    }

    @Override
    public List<CinemaScheduleApiResponseDto> getTheaterSchedule(String playYMD, List<String> movieIds, List<String> theaterIds) {

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
        
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "API TYPE=CGV MOVIE SCHEDULE, 영화 ID=" + movieIds + ", 극장 ID=" + theaterIds + " 오류=" + response.body());
            }
            String responseBody = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(responseBody);

            return parseScheduleResponse(root);
        } catch (Exception e) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "API TYPE=CGV MOVIE SCHEDULE, 영화 ID=" + movieIds + ", 극장 ID=" + theaterIds + " 오류=" + e.getMessage());
        }
    }

    private List<CinemaScheduleApiResponseDto> parseScheduleResponse(JsonNode root) throws JsonProcessingException {
        String innerJsonStr = root.get("d").asText();
        JsonNode scheduleList = objectMapper.readTree(innerJsonStr).get("ResultSchedule").get("ScheduleList");

        List<CinemaScheduleApiResponseDto> result = new ArrayList<>();
        for (JsonNode item : scheduleList) {
            String movieCode = item.path("MovieGroupCd").asText();
            Movie movie = movieService.fetchMovieByBrandMovieCode(brandName, movieCode);
            if (movie == null) {
                log.warn("{}에서 찾을 수 없는 영화 정보가 있습니다. MovieCode: {}, MovieName: {}", brandName, movieCode, item.path("MovieNmKor").asText());
                continue;
            }
            String startTime = item.path("PlayStartTm").asText();
            String endTime = item.path("PlayEndTm").asText();
            try {
                int remainSeat = item.path("SeatRemainCnt").asInt();
                int capacitySeat = item.path("SeatCapacity").asInt();
                if (remainSeat > capacitySeat || remainSeat <= 0 || capacitySeat <= 0) {
                    log.warn("{}에서 예약이 불가능한 상영 일정 정보가 있습니다. MovieCode: {}, MovieName: {}, RemainingSeat: {}, CapacitySeat: {}", brandName, movieCode, item.path("MovieNmKor").asText(), remainSeat, capacitySeat);
                    continue;
                }
            } catch (Exception e) {
                log.warn("{}에서 예약이 불가능한 상영 일정 정보가 있습니다. MovieCode: {}, MovieName: {}, RemainingSeat: {}, CapacitySeat: {}", brandName, movieCode, item.path("MovieNmKor").asText(), item.path("SeatRemainCnt").asText(), item.path("SeatCapacity").asText());
                continue;
            }

            CinemaScheduleApiResponseDto dto = new CinemaScheduleApiResponseDto(
                    brandService.getBrandInfo(brandName),
                    TheaterMapper.toSimplifiedTheaterDto(theaterService.getTheaterInfo(brandName, item.path("TheaterCd").asText())),
                    MovieMapper.toSimplifiedMovieDto(movie),
                    item.path("PlatformCd").asText(),
                    item.path("PlatformNm").asText(),
                    item.path("ScreenCd").asText(),
                    item.path("ScreenNm").asText(),
                    item.path("PlayYmd").asText(),
                    startTime.substring(0, 2) + ":" + startTime.substring(2),
                    endTime.substring(0, 2) + ":" + endTime.substring(2),
                    item.path("RunningTime").asText(),
                    item.path("SeatRemainCnt").asText(),
                    item.path("SeatCapacity").asText()
            );
            result.add(dto);
        }
        return result;
    }
}
