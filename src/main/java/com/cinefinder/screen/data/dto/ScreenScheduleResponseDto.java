package com.cinefinder.screen.data.dto;

import com.cinefinder.theater.data.Brand;
import com.cinefinder.theater.data.dto.SimplifiedTheaterDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScreenScheduleResponseDto {
    private Brand brand;
    private SimplifiedTheaterDto theater;
    private String movieCd;
    private String movieNmKor;
    private String movieNmEng;
    private String platformCd;
    private String platformNm;
    private String screenCd;
    private String screenNm;
    private String playYmd;
    private String playStartTime;
    private String playEndTime;
    private String runningTime;
    private String seatRemainCnt;
    private String seatCapacityCnt;
}
