package com.cinefinder.movie.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.util.internal.StringUtil;
import lombok.*;

import java.util.stream.Stream;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieResponseDto {
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
    private Long favoriteCount;        /* 좋아요 개수 */

    public boolean hasMissingRequiredField() {
        return Stream.of(nation, plotText, runtime, genre, releaseDate, ratingGrade, posters, stlls, vods)
            .anyMatch(StringUtil::isNullOrEmpty);
    }

    public void updateMissingRequiredField(MovieResponseDto movieResponseDto) {
        if (StringUtil.isNullOrEmpty(nation)) this.updateNation(movieResponseDto.getNation());
        if (StringUtil.isNullOrEmpty(plotText)) this.updatePlotText(movieResponseDto.getPlotText());
        if (StringUtil.isNullOrEmpty(runtime)) this.updateRuntime(movieResponseDto.getRuntime());
        if (StringUtil.isNullOrEmpty(genre)) this.updateGenre(movieResponseDto.getGenre());
        if (StringUtil.isNullOrEmpty(releaseDate)) this.updateReleaseDate(movieResponseDto.getReleaseDate());
        if (StringUtil.isNullOrEmpty(ratingGrade)) this.updateRatingGrade(movieResponseDto.getRatingGrade());
        if (StringUtil.isNullOrEmpty(posters)) this.updatePosters(movieResponseDto.getPosters());
        if (StringUtil.isNullOrEmpty(stlls)) this.updateStlls(movieResponseDto.getStlls());
        if (StringUtil.isNullOrEmpty(vods)) this.updateVods(movieResponseDto.getVods());
    }

    public void updateCodes(MovieResponseDto movieResponseDto) {
        updateCgvCode(movieResponseDto.getCgvCode());
        updateMegaBoxCode(movieResponseDto.getMegaBoxCode());
        updateLotteCinemaCode(movieResponseDto.getLotteCinemaCode());
    }

    public void updateStlls(String stlls) { this.stlls = stlls;}
    public void updateVods(String vods) { this.vods = vods; }
    public void updatePosters(String posters) { this.posters = posters; }
    public void updateMovieKey(String movieKey) { this.movieKey = movieKey; }
    public void updateMovieId(Long movieId) { this.movieId = movieId; }
    public void updateTitle(String title) { this.title = title; }
    public void updateReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public void updatePlotText(String plotText) { this.plotText = plotText; }
    public void updateNation(String nation) { this.nation = nation; }
    public void updateRuntime(String runtime) { this.runtime = runtime; }
    public void updateRatingGrade(String ratingGrade) { this.ratingGrade = ratingGrade; }
    public void updateGenre(String genre) { this.genre = genre; }
    public void updateFavoriteCount(Long favoriteCount) { this.favoriteCount = favoriteCount; }
    public void updateCgvCode(String cgvCode) { this.cgvCode = cgvCode; }
    public void updateMegaBoxCode(String megaBoxCode) { this.megaBoxCode = megaBoxCode; }
    public void updateLotteCinemaCode(String lotteCinemaCode) { this.lotteCinemaCode = lotteCinemaCode; }
}