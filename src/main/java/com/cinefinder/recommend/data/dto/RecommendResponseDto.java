package com.cinefinder.recommend.data.dto;

import com.cinefinder.movie.data.dto.MovieResponseDto;
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
    private MovieResponseDto movieResponseDto;

    public void updateRank(Long rank) {
        this.rank = rank;
    }
}