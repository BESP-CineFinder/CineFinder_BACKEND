package com.cinefinder.favorite.data.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FavoriteCheckRequestDto {
    private Long userId;
    private List<Long> movieId;
}
