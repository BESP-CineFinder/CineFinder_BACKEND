package com.cinefinder.movie.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieDetails {
    private String cgvCode;
    private String megaBoxCode;
    private String lotteCinemaCode;
    private String title;              /* 제목 */
    private String titleEng;           /* 영문 제목 */
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
}
