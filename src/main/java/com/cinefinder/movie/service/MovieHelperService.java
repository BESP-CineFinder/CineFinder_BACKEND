package com.cinefinder.movie.service;

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
import org.springframework.web.client.RestTemplate;

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
        List<MovieDetails> cgvMovieDetailsList;
        List<MovieDetails> cgvResult = new ArrayList<>();
        MultiValueMap<String, Object> cgvMap = new LinkedMultiValueMap<>();
        int i = 1;

        do {
            cgvMap.set("iPage", String.valueOf(i++));
            cgvMap.set("pageRow", "20");
            cgvMap.set("mtype", "now");
            cgvMap.set("morder", "TicketRate");
            cgvMap.set("mnowflag", "1");
            cgvMap.set("mdistype", "");
            cgvMap.set("flag", "MLIST");

            HttpHeaders cgvHeaders = new HttpHeaders();
            cgvHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> cgvRequest = new HttpEntity<>(cgvMap, cgvHeaders);

            String cgvResponse = restTemplate.postForObject(cgvRequestUrl, cgvRequest, String.class);

            cgvMovieDetailsList = UtilParse.extractCgvMovieList(cgvResponse);
            cgvResult.addAll(cgvMovieDetailsList);
        } while (!cgvMovieDetailsList.isEmpty());

        return cgvResult;
    }

    public List<MovieDetails> requestMovieMegaBoxApi() {
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
    }

    public List<MovieDetails> requestMovieLotteCinemaApi() {
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

            if (!StringUtil.isNullOrEmpty(cgvCode) && originMovieDetails != null) {
                originMovieDetails.setCgvCode(movieDetails.getCgvCode());
            }

            if (!StringUtil.isNullOrEmpty(megaBoxCode) && originMovieDetails != null) {
                originMovieDetails.setMegaBoxCode(movieDetails.getMegaBoxCode());
            }

            if (!StringUtil.isNullOrEmpty(lotteCinemaCode) && originMovieDetails != null) {
                originMovieDetails.setLotteCinemaCode(movieDetails.getLotteCinemaCode());
            }

            if (originMovieDetails != null && originMovieDetails.getTitle().length() >= title.length()) {
                String originTitle = distinctMap.get(normalizeMovieKey).getTitle();
                MovieDetails changeTitleMovieDetails = distinctMap.get(normalizeMovieKey);
                changeTitleMovieDetails.setTitle(originTitle);
                movieDetails = changeTitleMovieDetails;
            }

            distinctMap.putIfAbsent(normalizeMovieKey, movieDetails);
        }

        return distinctMap;
    }
}