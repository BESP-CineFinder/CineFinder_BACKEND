package com.cinefinder.movie.data.model;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BoxOffice {
    private String rank;        /* 순위 */
    private Long movieId;       /* 영화 ID */
    private String movieKey;    /* 정규화된 영화명 키 */
    private String movieNm;     /* 영화명 */

    public void updateRank(String rank) {
        this.rank = rank;
    }

    public void updateMovieId(Long movieId) {
        this.movieId = movieId;
    }
}