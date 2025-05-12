package com.cinefinder.movie.controller;

import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.dto.BoxOfficeResponseDto;
import com.cinefinder.movie.data.dto.MovieDetailsResponseDto;
import com.cinefinder.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/movie")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    @GetMapping("/daily-box-office")
    public ResponseEntity<BaseResponse<List<BoxOfficeResponseDto>>> fetchDailyBoxOfficeInfo() {
        return ResponseMapper.successOf(ApiStatus._OK, movieService.getDailyBoxOfficeInfo(), MovieController.class);
    }

    @GetMapping("/movie-details")
    public ResponseEntity<BaseResponse<MovieDetailsResponseDto>> fetchMovieDetails(
        @RequestParam(value = "movieKey") String movieKey,
        @RequestParam(value = "title") String title
    ) {
        return ResponseMapper.successOf(ApiStatus._OK, movieService.getMovieDetails(movieKey, title), MovieController.class);
    }
}