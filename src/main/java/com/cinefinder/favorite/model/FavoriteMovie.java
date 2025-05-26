package com.cinefinder.favorite.model;

import com.cinefinder.movie.data.model.MovieDetails;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FavoriteMovie {
    private Long movieId;
    private String title;
    private String poster;
    private MovieDetails movieDetails;
}