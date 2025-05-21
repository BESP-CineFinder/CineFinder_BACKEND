package com.cinefinder.movie.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.Movie;
import com.cinefinder.movie.data.model.MovieDetails;
import com.cinefinder.movie.data.repository.MovieRepository;
import com.cinefinder.movie.mapper.MovieMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class MovieDbSyncService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final MovieDetailService movieDetailService;
    private final MovieHelperService movieHelperService;
    private final MovieRepository movieRepository;

    public void fetchMultiplexMovieDetails() {
        try {
            List<MovieDetails> totalMovieDetails = movieHelperService.requestMultiplexMovieApi();

            Map<String, MovieDetails> map = movieHelperService.mergeAndDeduplicateMovieDetails(totalMovieDetails);
            for (Map.Entry<String, MovieDetails> entry : map.entrySet()) {
                MovieDetails movieDetails = entry.getValue();
                String movieKey = entry.getKey();
                String redisKey = "movieDetails:" + movieKey;
                String title = movieDetails.getTitle();

                MovieDetails response = movieDetailService.getMovieDetails(title);

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
            log.error("오류: {}", e.getMessage());
            log.error("stackTrace: {}", Arrays.toString(e.getStackTrace()));
            throw new CustomException(ApiStatus._READ_FAIL, "멀티플렉스 영화 상세정보 저장 중 오류 발생");
        }
    }


}
