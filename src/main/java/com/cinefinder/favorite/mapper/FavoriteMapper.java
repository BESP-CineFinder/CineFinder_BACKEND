package com.cinefinder.favorite.mapper;

import com.cinefinder.favorite.Favorite;
import com.cinefinder.favorite.data.dto.FavoriteRequestDto;
import com.cinefinder.favorite.model.FavoriteMovie;
import com.cinefinder.movie.data.Movie;
import io.netty.util.internal.StringUtil;

public class FavoriteMapper {
    public static FavoriteMovie toFavoriteMovie(Movie movie) {
        String posters = movie.getPosters();
        String poster = "";
        if (!StringUtil.isNullOrEmpty(posters)) poster = posters.split("\\|")[0];

        return FavoriteMovie.builder()
                .movieId(movie.getId())
                .title(movie.getTitle())
                .poster(poster)
                .build();
    }

    public static Favorite toEntity(FavoriteRequestDto dto) {
        return Favorite.builder()
                .userId(dto.getUserId())
                .movieId(dto.getMovieId())
                .build();
    }
}