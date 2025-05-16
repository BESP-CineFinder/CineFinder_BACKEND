package com.cinefinder.movie.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.Movie;
import com.cinefinder.movie.data.model.MovieDetails;
import com.cinefinder.movie.data.repository.MovieRepository;
import com.cinefinder.movie.mapper.MovieMapper;
import com.cinefinder.movie.util.UtilParse;
import com.cinefinder.movie.util.UtilString;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieDetailService {
    @Value("${api.kmdb.request-url}")
    private String kmdbRequestUrl;

    @Value("${api.kmdb.service-key}")
    private String kmdbServiceKey;

    @Value("${movie.cgv.name}")
    private String cgvBrandName;

    @Value("${movie.mega.name}")
    private String megaBrandName;

    @Value("${movie.lotte.name}")
    private String lotteBrandName;

    private final RestTemplate restTemplate = new RestTemplate();
    private final RedisTemplate<String, Object> redisTemplate;
    private final MovieHelperService movieHelperService;
    private final MovieRepository movieRepository;

    public MovieDetails getMovieDetails(String title) {
        ObjectMapper mapper = new ObjectMapper();
        String movieKey = UtilString.normalizeMovieKey(title);

        String redisKey = "movieDetails:" + movieKey;
        log.info("🔑 [영화 상세정보 캐시 조회] REDIS 키 이름 : {}", redisKey);

        if (redisTemplate.hasKey(redisKey)) {
            log.info("✅ {} 키 존재 ... 캐시된 데이터 조회", redisKey);
            
            Object object = redisTemplate.opsForHash().get(redisKey, movieKey);
            return mapper.convertValue(object, MovieDetails.class);
        } else {
            log.info("✅ {} 키 없음 ... 영화 상세정보 데이터베이스 조회", redisKey);
            
            return getMovieDetailsFromDB(movieKey, title);
        }
    }

    public MovieDetails getMovieDetailsFromDB(String movieKey, String title) {
        try {
            log.info("🔑 [영화 상세정보 데이터베이스 조회] 영화키 이름 : {}", movieKey);

            Optional<Movie> optionalMovie = movieRepository.findByTitle(movieKey);
            if (optionalMovie.isPresent()) {
                log.info("✅ 데이터 존재 ... 영화 상세정보 데이터베이스 조회");
                return MovieMapper.toMovieDetails(optionalMovie.get());
            } else {
                log.info("✅ 데이터 없음 ... KMDB API 호출 후 캐싱");
                return fetchMovieDetails(movieKey, title);
            }
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "영화 상세정보 데이터베이스 조회 중 오류 발생");
        }
    }

    public MovieDetails fetchMovieDetails(String movieKey, String title) {
        try {
            String redisKey = "movieDetails:" + movieKey;
            MovieDetails returnMovieDetails = null;
            log.info("🔑 [영화 상세정보 저장] REDIS 키 이름 : {}", redisKey);

            // 1. 요청 URL 생성
            String url = String.format(
                kmdbRequestUrl + "?collection=kmdb_new2&detail=Y&ServiceKey=%s&title=%s&sort=repRlsDate,1&listCount=1",
                kmdbServiceKey,
                URLEncoder.encode(title, StandardCharsets.UTF_8)
            );

            // 2. API 요청
            String response = restTemplate.getForObject(new URI(url), String.class);

            // 3. 저장 List 생성
            List<MovieDetails> movieDetailsList = UtilParse.extractMovieDetailsList(response);

            // 4. 응답 결과가 2개 이상이라면
            if (movieDetailsList.size() >= 2) {
                log.warn("❌ API 응답 결과가 2개 이상");

                for (MovieDetails movieDetails : movieDetailsList) log.warn("{}", movieDetails.getTitle());
                throw new IllegalArgumentException();
            }

            // 5. Redis 데이터 저장 및 만료일자 설정
            for (MovieDetails movieDetails : movieDetailsList) {
                log.info("⭕ 영화 상세정보 데이터 캐싱 성공");

                redisTemplate.opsForHash().put(redisKey, movieKey, movieDetails);
                redisTemplate.expire(redisKey, 1, TimeUnit.DAYS);
                returnMovieDetails = movieDetails;
            }

            return returnMovieDetails;
        } catch (IllegalArgumentException e) {
            throw new CustomException(ApiStatus._INTERNAL_SERVER_ERROR, "영화 상세정보 저장 중 API 응답 결과가 2개 이상으로 데이터 정합성 위배");
        } catch (URISyntaxException e) {
            throw new CustomException(ApiStatus._INVALID_URI_FORMAT, "영화 상세정보 저장 중 URI 구분 분석 오류 발생");
        } catch (RestClientException e) {
            throw new CustomException(ApiStatus._EXTERNAL_API_FAIL, "영화 상세정보 저장 중 KMDB API 호출 오류 발생");
        } catch (Exception e) {
            throw new CustomException(ApiStatus._OPERATION_FAIL, "영화 상세정보 저장 중 오류 발생");
        }
    }

    public void fetchMultiflexMovieDetailList() {
        List<MovieDetails> totalMovieDetails = new ArrayList<>();

        // 1. CGV API 요청
        try {
            totalMovieDetails = movieHelperService.requestMovieCgvApi();
        } catch (RestClientException e) { log.error("❌ CGV 영화목록 API 호출 실패"); }

        // 2. 메가박스 API 요청
        try {
            totalMovieDetails.addAll(movieHelperService.requestMovieMegaBoxApi());
        } catch (RestClientException e) { log.error("❌ 메가박스 영화목록 API 호출 실패"); }

        // 3. 롯데시네마 API 요청
        try {
            totalMovieDetails.addAll(movieHelperService.requestMovieLotteCinemaApi());
        } catch (RestClientException e) {log.error("❌ 롯데시네마 영화목록 API 호출 실패"); }

        // 4. 멀티플렉스 3사 응답 결과 중복 제거하여 병합
        Map<String, MovieDetails> map = movieHelperService.mergeAndDeduplicateMovieDetails(totalMovieDetails);

        // 5. KMDB API 요청 및 저장
        for (Map.Entry<String, MovieDetails> entry : map.entrySet()) {
            MovieDetails movieDetails = entry.getValue();
            String movieKey = entry.getKey();
            String redisKey = "movieDetails:" + movieKey;
            String title = movieDetails.getTitle();

            // API 요청
            MovieDetails response = getMovieDetails(title);

            // API 응답이 없을 경우 건너뛰기
            if (response == null) continue;

            // REDIS 각 멀티플렉스 영화코드 데이터 갱신
            MovieDetails originMovieDetails = (MovieDetails) redisTemplate.opsForHash().get(redisKey, movieKey);
            if (originMovieDetails != null) {
                originMovieDetails.setCgvCode(movieDetails.getCgvCode());
                originMovieDetails.setMegaBoxCode(movieDetails.getMegaBoxCode());
                originMovieDetails.setLotteCinemaCode(movieDetails.getLotteCinemaCode());

                redisTemplate.opsForHash().put(redisKey, movieKey, originMovieDetails);
            }

            // 엔티티로 변환 후 목록에 추가
            Movie movie = MovieMapper.toEntity(movieDetails, response);
            try {
                movieRepository.save(movie);
            } catch (Exception e) {
                log.warn("‼️ {} 중복된 영화명 존재", movie.getTitle());

                Optional<Movie> optionalOriginMovie = movieRepository.findByTitle(title);
                if (optionalOriginMovie.isPresent()) {
                    Movie originMovie = optionalOriginMovie.get();
                    originMovie.updateMovie(movie);

                    log.info("⭕ 중복 영화 정보 업데이트 완료 {}", movie.getTitle());
                } else {
                    log.error("❌ 중복 예외 후 기존 영화 조회 실패 {}", movie.getTitle());
                }
            }
        }
    }

    public Movie fetchMovieByBrandMovieCode(String brandName, String movieCode) {
        if (brandName.equals(cgvBrandName)) {
            return movieRepository.findByCgvCode(movieCode);
        } else if (brandName.equals(lotteBrandName)) {
            return movieRepository.findByLotteCinemaCode(movieCode);
        } else if (brandName.equals(megaBrandName)) {
            return movieRepository.findByMegaBoxCode(movieCode);
        } else {
            return null;
        }
    }
  
    public List<Movie> getFavoriteMovieList(List<Long> movieIdList) {
        try {
            return movieRepository.findByMovieIdList(movieIdList);
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "좋아요 등록한 영화 ID 목록 조회 중 오류 발생");
        }
    }
}