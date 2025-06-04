package com.cinefinder.favorite.mapper;

import com.cinefinder.favorite.data.entity.Favorite;
import com.cinefinder.favorite.data.dto.FavoriteRequestDto;
import com.cinefinder.favorite.data.dto.FavoriteMovieResponseDto;
import com.cinefinder.movie.data.entity.Movie;
import com.cinefinder.movie.mapper.MovieMapper;
import io.netty.util.internal.StringUtil;

public class FavoriteMapper {
    public static FavoriteMovieResponseDto toFavoriteMovie(Movie movie) {
        String posters = movie.getPosters();
        String poster = "";
        if (!StringUtil.isNullOrEmpty(posters)) poster = posters.split("\\|")[0];

        return FavoriteMovieResponseDto.builder()
                .movieId(movie.getId())
                .title(movie.getTitle())
                .poster(poster)
                .movieResponseDto(MovieMapper.toMovieDetails(movie))
                .build();
    }

    public static Favorite toEntity(FavoriteRequestDto dto) {
        return Favorite.builder()
                .userId(dto.getUserId())
                .movieId(dto.getMovieId())
                .build();
    }
}