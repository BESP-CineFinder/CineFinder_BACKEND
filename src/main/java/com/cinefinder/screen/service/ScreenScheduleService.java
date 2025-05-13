package com.cinefinder.screen.service;

import com.cinefinder.screen.data.dto.ScreenScheduleResponseDto;

import java.io.IOException;
import java.util.List;

public interface ScreenScheduleService {

    List<ScreenScheduleResponseDto> getTheaterSchedule(String playYMD, List<String> movieIds, List<String> theaterIds) throws IOException;
}
