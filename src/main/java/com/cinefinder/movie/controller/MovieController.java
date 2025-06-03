package com.cinefinder.movie.controller;

import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.dto.BoxOfficeResponseDto;
import com.cinefinder.movie.data.dto.MovieResponseDto;
import com.cinefinder.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/movie")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    @GetMapping("/box-office/daily")
    public ResponseEntity<BaseResponse<List<BoxOfficeResponseDto>>> getDailyBoxOfficeInfo() {
        return ResponseMapper.successOf(ApiStatus._OK, movieService.fetchDailyBoxOfficeInfo(), MovieController.class);
    }

    @GetMapping("/details")
    public ResponseEntity<BaseResponse<MovieResponseDto>> fetchMovieDetails(@RequestParam String title) {
        return ResponseMapper.successOf(ApiStatus._OK, movieService.fetchMovieDetails(title), MovieController.class);
    }

    @PostMapping("/details/multiplex")
    public ResponseEntity<BaseResponse<Void>> fetchMultiplexMovieDetails() {
        movieService.fetchMultiplexMovieDetails();
        return ResponseMapper.successOf(ApiStatus._OK, null, MovieController.class);
    }

    @GetMapping("/search")
    public ResponseEntity<BaseResponse<List<Long>>> search(@RequestParam String keyword) {
        return ResponseMapper.successOf(ApiStatus._OK, movieService.searchMovies(keyword), MovieController.class);
    }
}