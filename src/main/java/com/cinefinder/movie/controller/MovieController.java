package com.cinefinder.movie.controller;

import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.model.BoxOffice;
import com.cinefinder.movie.data.model.MovieDetails;
import com.cinefinder.movie.service.BoxOfficeService;
import com.cinefinder.movie.service.MovieDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/movie")
@RequiredArgsConstructor
public class MovieController {
    private final BoxOfficeService boxOfficeService;
    private final MovieDetailService movieDetailService;

    @GetMapping("/daily-box-office")
    public ResponseEntity<BaseResponse<List<BoxOffice>>> fetchDailyBoxOfficeInfo() {
        return ResponseMapper.successOf(ApiStatus._OK, boxOfficeService.getDailyBoxOfficeInfo(), MovieController.class);
    }

    @GetMapping("/movie-details")
    public ResponseEntity<BaseResponse<MovieDetails>> fetchMovieDetails(@RequestParam String title) {
        return ResponseMapper.successOf(ApiStatus._OK, movieDetailService.getMovieDetails(title), MovieController.class);
    }

    @PostMapping("/multiflexMovieDetails")
    public ResponseEntity<BaseResponse<Void>> fetchMultiflexMovieDetails() {
        movieDetailService.fetchMultiflexMovieDetailList();

        return ResponseMapper.successOf(ApiStatus._OK, null, MovieController.class);
    }
}