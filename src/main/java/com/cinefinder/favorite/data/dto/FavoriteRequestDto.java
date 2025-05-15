package com.cinefinder.favorite.data.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FavoriteRequestDto {
    private Long userId;
    private Long movieId;
}
