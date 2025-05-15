package com.cinefinder.favorite.mapper;

import com.cinefinder.favorite.data.Favorite;
import com.cinefinder.favorite.data.dto.FavoriteRequestDto;
import com.cinefinder.favorite.data.model.FavoriteMovie;
import com.cinefinder.movie.data.Movie;

public class FavoriteMapper {
    public static FavoriteMovie toFavoriteMovie(Movie movie) {
        return FavoriteMovie.builder()
            .movieId(movie.getId())
            .title(movie.getTitle())
            .posters(movie.getPosters())
            .build();
    }

    public static Favorite toEntity(FavoriteRequestDto dto) {
        return Favorite.builder()
            .userId(dto.getUserId())
            .movieId(dto.getMovieId())
            .build();
    }
}
