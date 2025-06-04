package com.cinefinder.movie.mapper;

import com.cinefinder.movie.data.entity.Movie;
import com.cinefinder.movie.data.dto.SimplifiedMovieDto;
import com.cinefinder.movie.data.dto.MovieResponseDto;

public class MovieMapper {
    public static MovieResponseDto toMovieDetails(Movie movie) {
        return MovieResponseDto.builder()
            .movieId(movie.getId())
            .cgvCode(movie.getCgvCode())
            .megaBoxCode(movie.getMegaBoxCode())
            .lotteCinemaCode(movie.getLotteCinemaCode())
            .title(movie.getTitle())
            .titleEng(movie.getTitleEng())
            .movieKey(movie.getMovieKey())
            .nation(movie.getNation())
            .genre(movie.getGenre())
            .posters(movie.getPosters())
            .stlls(movie.getStlls())
            .plotText(movie.getPlotText())
            .ratingGrade(movie.getRatingGrade())
            .releaseDate(movie.getReleaseDate())
            .runtime(movie.getRuntime())
            .directors(movie.getDirectors())
            .actors(movie.getActors())
            .vods(movie.getVods())
            .build();
    }

    public static Movie toEntity(MovieResponseDto movieResponseDto, MovieResponseDto response) {
        return Movie.builder()
            .movieKey(response.getMovieKey())
            .cgvCode(movieResponseDto.getCgvCode())
            .megaBoxCode(movieResponseDto.getMegaBoxCode())
            .lotteCinemaCode(movieResponseDto.getLotteCinemaCode())
            .title(movieResponseDto.getTitle())
            .titleEng(response.getTitleEng())
            .nation(response.getNation())
            .genre(response.getGenre())
            .posters(response.getPosters())
            .stlls(response.getStlls())
            .plotText(response.getPlotText())
            .ratingGrade(response.getRatingGrade())
            .releaseDate(response.getReleaseDate())
            .runtime(response.getRuntime())
            .directors(response.getDirectors())
            .actors(response.getActors())
            .vods(response.getVods())
            .build();
    }

    public static SimplifiedMovieDto toSimplifiedMovieDto(Movie movie) {
        String poster = (movie.getPosters() != null && !movie.getPosters().isEmpty())
                ? movie.getPosters().split("\\|")[0]
                : null;

        return new SimplifiedMovieDto(
            movie.getId(),
            movie.getTitle(),
            movie.getTitleEng(),
            movie.getRatingGrade(),
            poster
        );
    }
}
