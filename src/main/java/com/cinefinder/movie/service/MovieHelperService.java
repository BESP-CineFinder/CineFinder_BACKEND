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

    public List<MovieDetails> requestMovieCgvApi() {
        try {
            List<MovieDetails> cgvMovieDetailsList;
            List<MovieDetails> cgvResult = new ArrayList<>();
            MultiValueMap<String, Object> cgvMap = new LinkedMultiValueMap<>();
            cgvMap.set("pageRow", "20");
            cgvMap.set("mtype", "now");
            cgvMap.set("morder", "TicketRate");
            cgvMap.set("mnowflag", "1");
            cgvMap.set("mdistype", "");
            cgvMap.set("flag", "MLIST");
            int i = 1;

            do {
                cgvMap.set("iPage", String.valueOf(i++));

                HttpHeaders cgvHeaders = new HttpHeaders();
                cgvHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
                HttpEntity<MultiValueMap<String, Object>> cgvRequest = new HttpEntity<>(cgvMap, cgvHeaders);

                String cgvResponse = restTemplate.postForObject(cgvRequestUrl, cgvRequest, String.class);

                cgvMovieDetailsList = UtilParse.extractCgvMovieList(cgvResponse);
                cgvResult.addAll(cgvMovieDetailsList);
            } while (!cgvMovieDetailsList.isEmpty());

            return cgvResult;
        } catch (RestClientException e) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "CGV 영화목록 API 호출 실패");
        } catch (Exception e) {
            throw new CustomException(ApiStatus._INTERNAL_SERVER_ERROR, "CGV 영화목록 API 호출 중 오류 발생");
        }
    }

    public List<MovieDetails> requestMovieMegaBoxApi() {
        try {
            String jsonBody = "{"
                + "\"currentPage\":\"1\","
                + "\"recordCountPerPage\":\"100\","
                + "\"pageType\":\"ticketing\","
                + "\"ibxMovieNmSearch\":\"\","
                + "\"onairYn\":\"Y\","
                + "\"specialType\":\"\","
                + "\"specialYn\":\"N\""
                + "}";
            HttpHeaders megaBoxHeaders = new HttpHeaders();
            megaBoxHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> megaBoxRequest = new HttpEntity<>(jsonBody, megaBoxHeaders);

            String megaBoxResponse = restTemplate.postForObject(megaBoxRequestUrl, megaBoxRequest, String.class);

            return UtilParse.extractMegaBoxMovieList(megaBoxResponse);
        } catch (IOException e) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "메가박스 영화목록 API 호출 실패");
        } catch (Exception e) {
            throw new CustomException(ApiStatus._INTERNAL_SERVER_ERROR, "메가박스 영화목록 API 호출 중 알 수 없는 오류 발생");
        }
    }

    public List<MovieDetails> requestMovieLotteCinemaApi() {
        try {
            MultiValueMap<String, Object> lotteCinemaMap = new LinkedMultiValueMap<>();
            String paramList = "{\n"
                + "  \"MethodName\": \"GetMoviesToBe\",\n"
                + "  \"channelType\": \"HO\",\n"
                + "  \"osType\": \"Chrome\",\n"
                + "  \"osVersion\": \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36\",\n"
                + "  \"multiLanguageID\": \"KR\",\n"
                + "  \"division\": 1,\n"
                + "  \"moviePlayYN\": \"Y\",\n"
                + "  \"orderType\": \"1\",\n"
                + "  \"blockSize\": 100,\n"
                + "  \"pageNo\": 1,\n"
                + "  \"memberOnNo\": \"\",\n"
                + "  \"imgdivcd\": 2\n"
                + "}";
            lotteCinemaMap.add("paramList", paramList);

            HttpHeaders lotteCinemaHeaders = new HttpHeaders();
            lotteCinemaHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, Object>> lotteCinemaRequest = new HttpEntity<>(lotteCinemaMap, lotteCinemaHeaders);

            String lotteCinemaResponse = restTemplate.postForObject(lotteCinemaRequestUrl, lotteCinemaRequest, String.class);

            return UtilParse.extractLotteCinemaMovieList(lotteCinemaResponse);
        } catch (IOException e) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "롯데시네마 영화목록 API 호출 실패");
        } catch (Exception e) {
            throw new CustomException(ApiStatus._INTERNAL_SERVER_ERROR, "롯데시네마 영화목록 API 호출 중 알 수 없는 오류 발생");
        }
    }

    public Map<String, MovieDetails> mergeAndDeduplicateMovieDetails(List<MovieDetails> totalMovieDetails) {
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
                    originMovieDetails.setCgvCode(movieDetails.getCgvCode());
                }

                if (!StringUtil.isNullOrEmpty(megaBoxCode)) {
                    originMovieDetails.setMegaBoxCode(movieDetails.getMegaBoxCode());
                }

                if (!StringUtil.isNullOrEmpty(lotteCinemaCode)) {
                    originMovieDetails.setLotteCinemaCode(movieDetails.getLotteCinemaCode());
                }

                if (originMovieDetails.getTitle().length() >= title.length()) {
                    movieDetails = originMovieDetails;
                }
            }

            distinctMap.putIfAbsent(normalizeMovieKey, movieDetails);
        }

        return distinctMap;
    }
}