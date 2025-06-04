package com.cinefinder.recommend.controller;

import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.recommend.data.dto.RecommendResponseDto;
import com.cinefinder.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommend")
public class RecommendController {
    private final RecommendService recommendService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<RecommendResponseDto>>> getRecommendedMovieList() {
        return ResponseMapper.successOf(ApiStatus._OK, recommendService.getRecommendMovieList(), RecommendController.class);
    }
}