package com.cinefinder.screen.service;

import com.cinefinder.screen.data.dto.CinemaScheduleApiResponseDto;

import java.util.List;

public interface ScreenScheduleService {

    List<CinemaScheduleApiResponseDto> getTheaterSchedule(String playYMD, List<String> movieIds, List<String> theaterIds);
    String getBrandName();
}
