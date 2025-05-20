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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional(readOnly = true)
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
        try {
            ObjectMapper mapper = new ObjectMapper();
            String movieKey = UtilString.normalizeMovieKey(title);
            String redisKey = "movieDetails:" + movieKey;

            if (redisTemplate.hasKey(redisKey)) {
                Object object = redisTemplate.opsForHash().get(redisKey, movieKey);
                MovieDetails movieDetails = mapper.convertValue(object, MovieDetails.class);
                movieDetails.updateMovieId(movieRepository.findMovieIdByMovieKey(movieKey));
                return movieDetails;
            } else {
                return getMovieDetailsFromDB(movieKey, title);
            }
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "영화 상세정보 캐시 조회 중 오류 발생");
        }
    }

    public MovieDetails getMovieDetailsFromDB(String movieKey, String title) {
        try {
            Optional<Movie> optionalMovie = movieRepository.findByMovieKey(movieKey);
            if (optionalMovie.isPresent()) {
                return MovieMapper.toMovieDetails(optionalMovie.get());
            } else {
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
            String url = String.format(
                kmdbRequestUrl + "?collection=kmdb_new2&detail=Y&ServiceKey=%s&title=%s&sort=repRlsDate,1&listCount=1",
                kmdbServiceKey,
                URLEncoder.encode(title, StandardCharsets.UTF_8)
            );

            String response = restTemplate.getForObject(new URI(url), String.class);

            List<MovieDetails> movieDetailsList = UtilParse.extractMovieDetailsList(response, movieKey);
            for (MovieDetails movieDetails : movieDetailsList) {
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

    @Transactional
    public void fetchMultiplexMovieDetails() {
        try {
            List<MovieDetails> totalMovieDetails = movieHelperService.requestMultiplexMovieApi();

            Map<String, MovieDetails> map = movieHelperService.mergeAndDeduplicateMovieDetails(totalMovieDetails);
            for (Map.Entry<String, MovieDetails> entry : map.entrySet()) {
                MovieDetails movieDetails = entry.getValue();
                String movieKey = entry.getKey();
                String redisKey = "movieDetails:" + movieKey;
                String title = movieDetails.getTitle();

                MovieDetails response = getMovieDetails(title);

                if (response == null) continue;

                MovieDetails originMovieDetails = (MovieDetails) redisTemplate.opsForHash().get(redisKey, movieKey);
                if (originMovieDetails != null) {
                    originMovieDetails.updateCodes(movieDetails);
                    redisTemplate.opsForHash().put(redisKey, movieKey, originMovieDetails);
                }

                Movie movie = MovieMapper.toEntity(movieDetails, response);
                Optional<Movie> optionalOriginMovie = movieRepository.findByMovieKey(movieKey);
                if (optionalOriginMovie.isPresent()) {
                    movie.updateMovie(optionalOriginMovie.get());
                } else {
                    movieRepository.save(movie);
                }
            }
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "멀티플렉스 영화 상세정보 저장 중 오류 발생");
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