package com.cinefinder.movie.service;

import com.cinefinder.favorite.data.repository.FavoriteRepository;
import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.entity.Movie;
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
import java.util.List;
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

    private final RestTemplate restTemplate = new RestTemplate();
    private final RedisTemplate<String, Object> redisTemplate;
    private final MovieHelperService movieHelperService;
    private final MovieRepository movieRepository;
    private final FavoriteRepository favoriteRepository;

    public MovieDetails getMovieDetails(String title) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String movieKey = UtilString.normalizeMovieKey(title);
            String redisKey = "movieDetails:" + movieKey;

            if (redisTemplate.hasKey(redisKey)) {
                log.debug("✅ 영화 상세정보 캐시 조회 : {}", movieKey);
                Object object = redisTemplate.opsForHash().get(redisKey, movieKey);
                MovieDetails movieDetails = mapper.convertValue(object, MovieDetails.class);
                movieDetails.updateFavoriteCount(favoriteRepository.countByMovieId(movieDetails.getMovieId()));
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
                log.debug("✅ 영화 상세정보 DB 조회 : {}", movieKey);
                MovieDetails movieDetails = MovieMapper.toMovieDetails(optionalMovie.get());
                movieDetails.updateFavoriteCount(favoriteRepository.countByMovieId(movieDetails.getMovieId()));
                return movieDetails;
            } else {
                return fetchMovieDetails(movieKey, title);
            }
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "영화 상세정보 데이터베이스 조회 중 오류 발생");
        }
    }

    public MovieDetails fetchMovieDetails(String movieKey, String title) {
        try {
            log.debug("✅ 영화 상세정보 KMDB API 호출 {} : ", movieKey);
            String redisKey = "movieDetails:" + movieKey;
            MovieDetails returnMovieDetails = null;
            String url = String.format(
                    // TODO : 업보
                    kmdbRequestUrl + "?collection=kmdb_new2&detail=Y&ServiceKey=%s&title=%s&sort=repRlsDate,1&listCount=1",
                    kmdbServiceKey,
                    URLEncoder.encode(title, StandardCharsets.UTF_8)
            );

            String response = restTemplate.getForObject(new URI(url), String.class);

            List<MovieDetails> movieDetailsList = UtilParse.extractMovieDetailsList(response, movieKey);
            for (MovieDetails movieDetails : movieDetailsList) {
                if (movieDetails.hasMissingRequiredField()) {
                    MovieDetails daumDetails = movieHelperService.requestMovieDaumApi(title);
                    if (daumDetails != null) movieDetails.updateMissingRequiredField(daumDetails);
                }

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
}
