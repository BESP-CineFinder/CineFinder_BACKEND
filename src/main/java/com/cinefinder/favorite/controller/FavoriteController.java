package com.cinefinder.favorite.controller;

import com.cinefinder.favorite.data.dto.FavoriteCheckRequestDto;
import com.cinefinder.favorite.data.dto.FavoriteRequestDto;
import com.cinefinder.favorite.data.dto.FavoriteMovieResponseDto;
import com.cinefinder.favorite.data.dto.FavoriteResponseDto;
import com.cinefinder.favorite.service.FavoriteService;
import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping
    public ResponseEntity<BaseResponse<Void>> updateFavoriteInfo(@RequestBody FavoriteRequestDto favoriteRequestDto) {
        favoriteService.updateFavoriteInfo(favoriteRequestDto);
        return ResponseMapper.successOf(ApiStatus._OK, null, FavoriteController.class);
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<FavoriteResponseDto>>> checkFavorite(
            @ModelAttribute FavoriteCheckRequestDto favoriteCheckRequestDto
    ) {
        return ResponseMapper.successOf(ApiStatus._OK, favoriteService.checkFavorite(favoriteCheckRequestDto), FavoriteController.class);
    }

    @GetMapping("/movie-list")
    public ResponseEntity<BaseResponse<List<FavoriteMovieResponseDto>>> getFavoriteMovieListByUser(@RequestParam Long userId) {
        return ResponseMapper.successOf(ApiStatus._OK, favoriteService.getFavoriteMovieListByUser(userId), FavoriteController.class);
    }
}