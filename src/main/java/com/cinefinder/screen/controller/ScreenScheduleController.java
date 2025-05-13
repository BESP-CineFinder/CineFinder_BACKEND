package com.cinefinder.screen.controller;

import com.cinefinder.screen.data.dto.ScreenScheduleRequestDto;
import com.cinefinder.screen.data.dto.ScreenScheduleResponseDto;
import com.cinefinder.screen.service.CgvScreenScheduleServiceImpl;
import com.cinefinder.screen.service.MegaScreenScheduleServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/screen")
public class ScreenScheduleController {

    private final CgvScreenScheduleServiceImpl cgvScreenScheduleServiceImpl;
    private final MegaScreenScheduleServiceImpl megaScreenScheduleServiceImpl;

    @GetMapping("/schedule")
    public List<ScreenScheduleResponseDto> getTheaterSchedule(@RequestBody ScreenScheduleRequestDto requestDto) throws IOException {
        return megaScreenScheduleServiceImpl.getTheaterSchedule(
                requestDto.getPlayYMD(),
                requestDto.getMovieIds(),
                requestDto.getTheaterIds()
        );
    }
}
