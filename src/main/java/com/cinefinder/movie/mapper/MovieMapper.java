package com.cinefinder.movie.mapper;

import com.cinefinder.movie.data.Movie;
import com.cinefinder.movie.data.SimplifiedMovieDto;
import com.cinefinder.movie.data.model.MovieDetails;

public class MovieMapper {
    public static MovieDetails toMovieDetails(Movie movie) {
        return MovieDetails.builder()
            .cgvCode(movie.getCgvCode())
            .megaBoxCode(movie.getMegaBoxCode())
            .lotteCinemaCode(movie.getLotteCinemaCode())
            .title(movie.getTitle())
            .titleEng(movie.getTitleEng())
            .nation(movie.getNation())
            .genre(movie.getGenre())
            .posters(movie.getPosters())
            .stlls(movie.getStlls())
            .plotText(movie.getPlotText())
            .ratingGrade(movie.getRatingGrade())
            .releaseDate(movie.getReleaseDate())
            .runtime(movie.getRuntime())
            .directors(movie.getCgvCode())
            .actors(movie.getCgvCode())
            .vods(movie.getCgvCode())
            .build();
    }

    public static Movie toEntity(MovieDetails movieDetails, MovieDetails response) {
        return Movie.builder()
            .cgvCode(movieDetails.getCgvCode())
            .megaBoxCode(movieDetails.getMegaBoxCode())
            .lotteCinemaCode(movieDetails.getLotteCinemaCode())
            .title(movieDetails.getTitle())
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
        return new SimplifiedMovieDto(
            movie.getId(),
            movie.getTitle(),
            movie.getTitleEng(),
            movie.getRatingGrade(),
            movie.getPosters().split("\\|")[0]
        );
    }
}
