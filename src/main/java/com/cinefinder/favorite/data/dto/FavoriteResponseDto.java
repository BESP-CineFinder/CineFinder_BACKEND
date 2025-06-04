package com.cinefinder.favorite.data.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FavoriteResponseDto {
    private Long userId;
    private Long movieId;
    private boolean isFavorite;
}
