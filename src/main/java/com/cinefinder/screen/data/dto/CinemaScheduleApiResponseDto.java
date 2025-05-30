package com.cinefinder.screen.data.dto;

import com.cinefinder.movie.data.SimplifiedMovieDto;
import com.cinefinder.brand.data.Brand;
import com.cinefinder.theater.data.dto.SimplifiedTheaterDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CinemaScheduleApiResponseDto {
    private Brand brand;
    private SimplifiedTheaterDto theater;
    private SimplifiedMovieDto movie;
    private String multiplexMovieCode;
    private String filmCd;
    private String filmNm;
    private String screenCd;
    private String screenNm;
    private String playYmd;
    private String playStartTime;
    private String playEndTime;
    private String runningTime;
    private String seatRemainCnt;
    private String seatCapacityCnt;
}
