package com.cinefinder.screen.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScreenScheduleResponseDto {
    private String brandNm;
    private String theaterCd;
    private String theaterNm;
    private String movieCd;
    private String movieNmKor;
    private String movieNmEng;
    private String movieRatingCd;
    private String movieRatingNm;
    private String screenCd;
    private String screenNm;
    private String playYmd;
    private String playStartTm;
    private String playEndTm;
    private String runningTime;
    private String seatRemainCnt;
    private String seatCapacity;
    private String platformCd;
    private String platformNm;
    private String posterImageUrl;
}
