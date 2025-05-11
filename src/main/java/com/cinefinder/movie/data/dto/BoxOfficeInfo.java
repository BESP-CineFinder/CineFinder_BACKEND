package com.cinefinder.movie.data.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BoxOfficeInfo {
    @Setter
    private String rank;        /* 순위 */
    private String movieKey;    /* 정규화된 영화명 키 */
    private String movieNm;     /* 영화명 */
    private String openDt;      /* 개봉일자 */
}