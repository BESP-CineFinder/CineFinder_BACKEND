package com.cinefinder.movie.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SimplifiedMovieDto {

    private long id;
    private String titleKor;
    private String titleEng;
    private String ratingGrade;
    private String poster;
}
