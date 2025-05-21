package com.cinefinder.movie.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.model.MovieDetails;
import com.cinefinder.movie.util.UtilParse;
import com.cinefinder.movie.util.UtilString;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MovieHelperService {
    @Value("${api.cgv.code.request-url}")
    private String cgvRequestUrl;

    @Value("${api.mega-box.code.request-url}")
    private String megaBoxRequestUrl;

    @Value("${api.lotte-cinema.code.request-url}")
    private String lotteCinemaRequestUrl;

    private static final List<String> IGNORE_TITLE_LIST = List.of("AD");
    private final RestTemplate restTemplate = new RestTemplate();

    public List<MovieDetails> requestMultiplexMovieApi() {
        List<MovieDetails> multiplexMovieList = new ArrayList<>();
        multiplexMovieList.addAll(requestMovieCgvApi());
        multiplexMovieList.addAll(requestMovieMegaBoxApi());
        multiplexMovieList.addAll(requestMovieLotteCinemaApi());

        return multiplexMovieList;
    }

    public List<MovieDetails> requestMovieCgvApi() {
        try {
            String cgvResponse = restTemplate.getForObject(cgvRequestUrl, String.class);

            return UtilParse.extractCgvMovieList(cgvResponse);
        } catch (RestClientException e) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "CGV 영화목록 API 호출 실패");
        } catch (Exception e) {
            throw new CustomException(ApiStatus._INTERNAL_SERVER_ERROR, "CGV 영화목록 API 호출 중 알 수 없는 오류 발생");
        }
    }

    public List<MovieDetails> requestMovieMegaBoxApi() {
        try {
            String jsonBody = "{"
                + "\"currentPage\":\"1\","
                + "\"recordCountPerPage\":\"150\","
                + "\"pageType\":\"ticketing\","
                + "\"ibxMovieNmSearch\":\"\","
                + "\"onairYn\":\"Y\","
                + "\"specialType\":\"\","
                + "\"specialYn\":\"N\""
                + "}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            String response = restTemplate.postForObject(megaBoxRequestUrl, request, String.class);

            return UtilParse.extractMegaBoxMovieList(response);
        } catch (IOException e) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "메가박스 영화목록 API 호출 실패");
        } catch (Exception e) {
            throw new CustomException(ApiStatus._INTERNAL_SERVER_ERROR, "메가박스 영화목록 API 호출 중 알 수 없는 오류 발생");
        }
    }

    public List<MovieDetails> requestMovieLotteCinemaApi() {
        try {
            List<MovieDetails> movieDetailsList = new ArrayList<>();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String paramList = """
                {
                  "MethodName": "GetMoviesToBe",
                  "channelType": "HO",
                  "osType": "Chrome",
                  "osVersion": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36",
                  "multiLanguageID": "KR",
                  "division": 1,
                  "moviePlayYN": "%s",
                  "orderType": "%s",
                  "blockSize": 100,
                  "pageNo": 1,
                  "memberOnNo": "",
                  "imgdivcd": 2
                }
            """;

            String[][] requestParams = {
                    {"N", "5"},
                    {"Y", "1"}
            };

            for (String[] params : requestParams) {
                String formattedParam = String.format(paramList, params[0], params[1]);
                MultiValueMap<String, Object> payload = new LinkedMultiValueMap<>();
                payload.add("paramList", formattedParam);

                HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(payload, headers);
                String response = restTemplate.postForObject(lotteCinemaRequestUrl, request, String.class);

                movieDetailsList.addAll(UtilParse.extractLotteCinemaMovieList(response));
            }

            return movieDetailsList;
        } catch (IOException e) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "롯데시네마 영화목록 API 호출 실패");
        } catch (Exception e) {
            throw new CustomException(ApiStatus._INTERNAL_SERVER_ERROR, "롯데시네마 영화목록 API 호출 중 알 수 없는 오류 발생");
        }
    }

    public Map<String, MovieDetails> mergeAndDeduplicateMovieDetails(List<MovieDetails> totalMovieDetails) {
        try {
            Map<String, MovieDetails> distinctMap = new HashMap<>();
            for (MovieDetails movieDetails : totalMovieDetails) {
                String normalizeMovieKey = UtilString.normalizeMovieKey(movieDetails.getTitle());
                String title = movieDetails.getTitle();
                String cgvCode = movieDetails.getCgvCode();
                String megaBoxCode = movieDetails.getMegaBoxCode();
                String lotteCinemaCode = movieDetails.getLotteCinemaCode();
                MovieDetails originMovieDetails = distinctMap.get(normalizeMovieKey);

                if (IGNORE_TITLE_LIST.contains(title)) continue;

                if (originMovieDetails != null) {
                    if (!StringUtil.isNullOrEmpty(cgvCode)) {
                        originMovieDetails.updateCgvCode(movieDetails.getCgvCode());
                    }

                    if (!StringUtil.isNullOrEmpty(megaBoxCode)) {
                        originMovieDetails.updateMegaBoxCode(movieDetails.getMegaBoxCode());
                    }

                    if (!StringUtil.isNullOrEmpty(lotteCinemaCode)) {
                        originMovieDetails.updateLotteCinemaCode(movieDetails.getLotteCinemaCode());
                    }

                    if (originMovieDetails.getTitle().length() >= title.length()) {
                        movieDetails = originMovieDetails;
                    }
                }

                distinctMap.putIfAbsent(normalizeMovieKey, movieDetails);
            }

            return distinctMap;
        } catch (Exception e) {
            throw new CustomException(ApiStatus._INTERNAL_SERVER_ERROR, "멀티플렉스 영화목록 병합 중 알 수 없는 오류 발생");
        }
    }
}