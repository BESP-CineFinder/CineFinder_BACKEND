package com.cinefinder.movie.controller;

import com.cinefinder.movie.data.dto.BoxOfficeInfo;
import com.cinefinder.movie.data.dto.MovieDetails;
import com.cinefinder.movie.service.MovieService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping(value = "/api")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    @GetMapping("/daily-box-office")
    public List<BoxOfficeInfo> fetchDailyBoxOfficeInfo() throws IOException, URISyntaxException {
        return movieService.getDailyBoxOfficeInfo();
    }

    @GetMapping("/movie-details")
    public MovieDetails fetchMovieDetails(
        @RequestParam(value = "movieKey") String movieKey,
        @RequestParam(value = "title") String title,
        @RequestParam(value = "releaseDts") String releaseDts
    ) throws URISyntaxException, JsonProcessingException {
        return movieService.getMovieDetails(movieKey, title, releaseDts);
    }
}