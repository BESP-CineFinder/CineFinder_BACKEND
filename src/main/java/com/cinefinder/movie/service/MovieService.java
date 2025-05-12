package com.cinefinder.movie.service;

import com.cinefinder.movie.data.dto.BoxOfficeInfo;
import com.cinefinder.movie.data.dto.MovieDetails;
import com.cinefinder.movie.data.type.ConvertType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {
    @Value("${api.kobis.request-url}")
    private String kobisRequestUrl;

    @Value("${api.kobis.service-key}")
    private String kobisServiceKey;

    @Value("${api.kmdb.request-url}")
    private String kmdbRequestUrl;

    @Value("${api.kmdb.service-key}")
    private String kmdbServiceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final RedisTemplate<String, Object> redisTemplate;

    public List<BoxOfficeInfo> getDailyBoxOfficeInfo() {
        ObjectMapper mapper = new ObjectMapper();

        // 1. 최신 일자 계산
        String latestDay = convert(LocalDate.now().minusDays(1).toString(), ConvertType.DATE);
        String redisKey = "dailyBoxOffice:" + latestDay;
        log.info("🔑 [일간 박스오피스 정보 조회] REDIS 키 이름 : {}", redisKey);

        // 2. 일간 박스오피스 정보 응답 분기 처리
        if (redisTemplate.hasKey(redisKey)) {
            log.info("✅ {} 키 존재 ... 캐시된 데이터 조회", redisKey);

            return redisTemplate.opsForHash()
                .entries(redisKey)
                .entrySet()
                .stream()
                .map(entry -> {
                    String rank = entry.getKey().toString();
                    BoxOfficeInfo boxOfficeInfo = mapper.convertValue(entry.getValue(), BoxOfficeInfo.class);
                    boxOfficeInfo.setRank(rank);
                    return boxOfficeInfo;
                })
                .sorted(Comparator.comparingInt(info -> Integer.parseInt(info.getRank())))
                .collect(Collectors.toList());
        } else {
            log.info("✅ {} 키 없음 ... KOBIS API 호출 후 캐싱", redisKey);

            return fetchDailyBoxOfficeInfo();
        }
    }

    public List<BoxOfficeInfo> fetchDailyBoxOfficeInfo() {
        try {
            // 1. 최신 일자 계산
            String latestDay = convert(LocalDate.now().minusDays(1).toString(), ConvertType.DATE);
            String redisKey = "dailyBoxOffice:" + latestDay;
            log.info("🔑 [일간 박스오피스 정보 저장] REDIS 키 이름 : {}", redisKey);

            // 2. 요청 URL 생성
            String url = String.format(
                    kobisRequestUrl + "?key=%s&targetDt=%s",
                    kobisServiceKey,
                    latestDay
            );

            // 3. API 요청
            String response = restTemplate.getForObject(new URI(url), String.class);

            // 4. 요청 및 응답 List 생성
            List<BoxOfficeInfo> dailyBoxOfficeInfoList = extractDailyBoxOfficeInfoList(response);

            // 5. Redis 데이터 저장
            for (BoxOfficeInfo boxOfficeInfo : dailyBoxOfficeInfoList) {
                redisTemplate.opsForHash().put(redisKey, boxOfficeInfo.getRank(), boxOfficeInfo);
            }
            log.info("⭕ REDIS 저장 완료");

            return dailyBoxOfficeInfoList;
        } catch (Exception e) {
            throw new RuntimeException("일간 박스오피스 정보 저장 중 오류 발생", e);
        }
    }

    public MovieDetails getMovieDetails(String movieKey, String title, String releaseDate) {
        ObjectMapper mapper = new ObjectMapper();

        String redisKey = "movieDetails:" + movieKey;
        log.info("🔑 [영화 상세정보 조회] REDIS 키 이름 : {}", redisKey);

        if (redisTemplate.hasKey(redisKey)) {
            log.info("✅ {} 키 존재 ... 캐시된 데이터 조회", redisKey);
            
            Object object = redisTemplate.opsForHash().get(redisKey, movieKey);
            return mapper.convertValue(object, MovieDetails.class);
        } else {
            log.info("✅ {} 키 없음 ... KMDB API 호출 후 캐싱", redisKey);
            
            return fetchMovieDetails(movieKey, title, releaseDate);
        }
    }

    public MovieDetails fetchMovieDetails(String movieKey, String title, String releaseDate) {
        try {
            String redisKey = "movieDetails:" + movieKey;
            MovieDetails returnMovieDetails = null;
            log.info("🔑 [영화 상세정보 저장] REDIS 키 이름 : {}", redisKey);

            // 1. 요청 URL 생성
            String url = String.format(
                kmdbRequestUrl + "?collection=kmdb_new2&detail=Y&ServiceKey=%s&title=%s&releaseDts=%s&releaseDte=%s",
                kmdbServiceKey,
                URLEncoder.encode(title, StandardCharsets.UTF_8),
                releaseDate,
                releaseDate
            );

            // 2. API 요청
            String response = restTemplate.getForObject(new URI(url), String.class);

            // 3. 저장 List 생성
            List<MovieDetails> movieDetailsList = extractMovieDetailsList(response);

            // 4. 응답 결과가 2개 이상이라면
            if (movieDetailsList.size() >= 2) {
                log.warn("❌ API 1개의 요청 파라미터에 응답 결과가 2개 이상");

                for (MovieDetails movieDetails : movieDetailsList) log.warn("{}", movieDetails.getTitle());
                throw new IllegalArgumentException("영화 상세정보 데이터 캐싱 전 프로세스 중단");
            }

            // 5. Redis 데이터 저장
            for (MovieDetails movieDetails : movieDetailsList) {
                log.info("⭕ 영화 상세정보 데이터 캐싱 성공");

                redisTemplate.opsForHash().put(redisKey, movieKey, movieDetails);
                returnMovieDetails = movieDetails;
            }

            return returnMovieDetails;
        } catch (Exception e) {
            throw new RuntimeException("영화 상세정보 저장 중 오류 발생", e);
        }
    }

    private List<BoxOfficeInfo> extractDailyBoxOfficeInfoList(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // 1. 일간 박스오피스 목록 파싱
            JsonNode root = mapper.readTree(response);
            JsonNode dailyBoxOfficeList = root.path("boxOfficeResult")
                    .path("dailyBoxOfficeList");

            // 2. 요청 및 응답 List 생성
            List<BoxOfficeInfo> list = new ArrayList<>();
            for (JsonNode node : dailyBoxOfficeList) {
                String movieNm = node.path("movieNm").asText();

                BoxOfficeInfo boxOfficeInfo = BoxOfficeInfo.builder()
                        .rank(node.path("rank").asText())
                        .movieNm(movieNm)
                        .movieKey(convert(movieNm, ConvertType.MOVIE_KEY))
                        .openDt(convert(node.path("openDt").asText(), ConvertType.DATE))
                        .build();

                list.add(boxOfficeInfo);
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException("일간 박스오피스 목록 추출 중 오류 발생", e);
        }
    }

    private List<MovieDetails> extractMovieDetailsList(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<MovieDetails> list = new ArrayList<>();

            // 1. 일간 박스오피스 목록 파싱
            JsonNode root = mapper.readTree(response);
            JsonNode dataList = root.path("Data").get(0);
            JsonNode result = dataList.path("Result");

            // 2. 응답 결과가 없다면
            if (result.isMissingNode()) {
                log.warn("❌ API 응답 결과가 없음");
                return list;
            }

            // 3. 요청 및 응답 List 생성
            for (JsonNode node : result) {
                String plotText = "";
                String runtime = "";
                String ratingGrade = "";
                String releaseDate = "";

                List<String> directors = new ArrayList<>();
                List<String> actors = new ArrayList<>();
                List<String> vods = new ArrayList<>();

                String title = node.path("title").asText();
                String titleEng = node.path("titleEng").asText();
                String nation = node.path("nation").asText();
                String genre = node.path("genre").asText();
                String posters = node.path("posters").asText();
                String stlls = node.path("stlls").asText();

                JsonNode plotTextNodes = node.path("plots").path("plot");
                for (JsonNode plotTextNode : plotTextNodes) plotText = plotTextNode.path("plotText").asText();

                JsonNode ratingNodes = node.path("ratings").path("rating");
                for (JsonNode ratingNode : ratingNodes) {
                    runtime = convert(ratingNode.path("runtime").asText(), ConvertType.KMDB_JSON_NODE);
                    ratingGrade = convert(ratingNode.path("ratingGrade").asText(), ConvertType.KMDB_JSON_NODE);
                    releaseDate = convert(ratingNode.path("releaseDate").asText(), ConvertType.KMDB_JSON_NODE);
                }

                JsonNode directorsNodes = node.path("directors").path("director");
                for (JsonNode directorNode : directorsNodes) directors.add(directorNode.path("directorNm").asText());

                JsonNode actorsNodes = node.path("actors").path("actor");
                for (JsonNode actorNode : actorsNodes) actors.add(actorNode.path("actorNm").asText());

                JsonNode vodsNode = node.path("vods").path("vod");
                for (JsonNode vod : vodsNode) vods.add(vod.path("vodUrl").asText());

                MovieDetails movieDetails = MovieDetails.builder()
                    .title(title)
                    .titleEng(titleEng)
                    .nation(nation)
                    .genre(genre)
                    .directors(directors)
                    .actors(actors)
                    .plotText(plotText)
                    .runtime(runtime)
                    .ratingGrade(ratingGrade)
                    .releaseDate(releaseDate)
                    .posters(posters)
                    .stlls(stlls)
                    .vods(vods)
                    .build();

                list.add(movieDetails);
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException("영화 상세정보 추출 중 오류 발생", e);
        }
    }

    private String convert(String input, ConvertType type) {
        if (StringUtil.isNullOrEmpty(input)) throw new IllegalArgumentException("문자열 변환 중 오류 발생 ... NULL 또는 빈값");

        switch (type) {
            case ConvertType.MOVIE_KEY -> {
                return input.toLowerCase()
                        .replaceAll("[^\\p{IsHangul}\\p{IsAlphabetic}\\p{IsDigit}\\s]", "")
                        .replaceAll("\\s+", "")
                        .trim();
            }

            case ConvertType.DATE -> {
                return input.replaceAll("-", "");
            }

            case ConvertType.KMDB_JSON_NODE -> {
                if (input.contains("||")) {
                    String[] parts = input.split("\\|\\|");
                    return parts[parts.length - 1].replaceAll("-", "");
                } else {
                    return input.replaceAll("-", "");
                }
            }

            default -> throw new IllegalArgumentException("문자열 변환 중 오류 발생 ... 지원하지 않는 변환 타입");
        }
    }
}