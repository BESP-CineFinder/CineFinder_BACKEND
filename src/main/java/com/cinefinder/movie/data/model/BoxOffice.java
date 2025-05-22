package com.cinefinder.movie.data.model;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BoxOffice {
    private String rank;               /* 순위 */
    private Long movieId;              /* 영화 ID */
    private String movieKey;           /* 정규화된 영화명 키 */
    private String movieNm;            /* 영화명 */
    private MovieDetails movieDetails; /* 영화상세정보 */

    public void updateRank(String rank) {
        this.rank = rank;
    }

    public void updateMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public void updateMovieDetails(MovieDetails movieDetails) {
        this.movieDetails = movieDetails;
    }
}