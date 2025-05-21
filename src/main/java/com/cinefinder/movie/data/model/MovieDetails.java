package com.cinefinder.movie.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieDetails {
    private Long movieId;              /* 영화 ID */
    private String cgvCode;            /* CGV 영화 코드 */
    private String megaBoxCode;        /* 메가박스 영화 코드 */
    private String lotteCinemaCode;    /* 롯데시네마 영화 코드 */
    private String title;              /* 제목 */
    private String titleEng;           /* 영문 제목 */
    private String movieKey;           /* 정규화된 제목 */
    private String nation;             /* 국가 */
    private String genre;              /* 장르 */
    private String posters;            /* 포스터 URL 목록 */
    private String stlls;              /* 스틸컷 URL 목록 */
    private String plotText;           /* 플롯 */
    private String ratingGrade;        /* 심의 정보 */
    private String releaseDate;        /* 개봉일자 */
    private String runtime;            /* 상영 시간 */
    private String directors;          /* 감독 */
    private String actors;             /* 배우 */
    private String vods;               /* VOD URL 목록 */

    public void updateMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateCodes(MovieDetails movieDetails) {
        updateCgvCode(movieDetails.getCgvCode());
        updateMegaBoxCode(movieDetails.getMegaBoxCode());
        updateLotteCinemaCode(movieDetails.getLotteCinemaCode());
    }

    public void updateCgvCode(String cgvCode) {
        this.cgvCode = cgvCode;
    }

    public void updateMegaBoxCode(String megaBoxCode) {
        this.megaBoxCode = megaBoxCode;
    }

    public void updateLotteCinemaCode(String lotteCinemaCode) {
        this.lotteCinemaCode = lotteCinemaCode;
    }
}
