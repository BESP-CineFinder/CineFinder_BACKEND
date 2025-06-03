package com.cinefinder.movie.service;

import com.cinefinder.chat.service.ChatLogElasticService;
import com.cinefinder.chat.service.KafkaService;
import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.entity.Movie;
import com.cinefinder.movie.data.dto.MovieResponseDto;
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
    private final KafkaService kafkaService;
    private final ChatLogElasticService chatLogElasticService;

    public void fetchMultiplexMovieDetails() {
        try {
            List<MovieResponseDto> totalMovieDetails = movieHelperService.requestMultiplexMovieApi();

            Map<String, MovieResponseDto> map = movieHelperService.mergeAndDeduplicateMovieDetails(totalMovieDetails);
            for (Map.Entry<String, MovieResponseDto> entry : map.entrySet()) {
                MovieResponseDto movieResponseDto = entry.getValue();
                String movieKey = entry.getKey();
                String redisKey = "movieDetails:" + movieKey;
                String title = movieResponseDto.getTitle();

                MovieResponseDto response = resolveMovieDetails(movieKey, title);

                if (response == null) continue;

                Movie movie = MovieMapper.toEntity(movieResponseDto, response);
                Optional<Movie> optionalOriginMovie = movieRepository.findByMovieKey(movieKey);
                if (optionalOriginMovie.isPresent()) {
                    Movie originMovie = optionalOriginMovie.get();
                    originMovie.updateMovie(movie);
                } else {
                    movieRepository.save(movie);
                    log.info("✅ 영화 상세정보 저장 완료 : {}", movie.getTitle());
                    kafkaService.createTopicIfNotExists(movie.getId().toString());
                    chatLogElasticService.createElasticsearchChatIndex(movie.getId().toString());
                    chatLogElasticService.createElasticsearchSentimentIndex(movie.getId().toString());
                }

                MovieResponseDto originMovieResponseDto = (MovieResponseDto) redisTemplate.opsForHash().get(redisKey, movieKey);
                if (originMovieResponseDto != null) {
                    originMovieResponseDto.updateCodes(movieResponseDto);
                    originMovieResponseDto.updateMovieId(movieRepository.findMovieIdByMovieKey(movieKey));
                    originMovieResponseDto.updateMissingRequiredField(response);
                    redisTemplate.opsForHash().put(redisKey, movieKey, originMovieResponseDto);
                }
            }
        } catch (Exception e) {
            log.error("오류: {}", e.getMessage());
            log.error("stackTrace: {}", Arrays.toString(e.getStackTrace()));
            throw new CustomException(ApiStatus._READ_FAIL, "멀티플렉스 영화 상세정보 저장 중 오류 발생");
        }
    }

    public MovieResponseDto resolveMovieDetails(String movieKey, String title) {
        MovieResponseDto movieResponseDto = movieDetailService.fetchMovieDetails(movieKey, title);

        if (movieResponseDto != null && movieResponseDto.hasMissingRequiredField()) {
            MovieResponseDto daumDetails = movieHelperService.requestMovieDaumApi(title);
            if (daumDetails != null) movieResponseDto.updateMissingRequiredField(daumDetails);
        }

        return movieResponseDto;
    }
}
