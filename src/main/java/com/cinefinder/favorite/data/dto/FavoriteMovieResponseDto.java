package com.cinefinder.favorite.data.dto;

import com.cinefinder.movie.data.dto.MovieResponseDto;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FavoriteMovieResponseDto {
    private Long movieId;
    private String title;
    private String poster;
    private MovieResponseDto movieResponseDto;
}