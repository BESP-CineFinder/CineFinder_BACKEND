package com.cinefinder.recommend.data.dto;

import com.cinefinder.movie.data.model.MovieDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendResponseDto {
    private Long movieId;
    private Long rank;
    private Double score;
    private MovieDetails movieDetails;

    public void updateRank(Long rank) {
        this.rank = rank;
    }
}