package com.cinefinder.screen.controller;

import com.cinefinder.screen.data.dto.ScreenScheduleRequestDto;
import com.cinefinder.screen.data.dto.ScreenScheduleResponseDto;
import com.cinefinder.screen.service.CgvScreenScheduleService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
public class CgvScreenScheduleController {

    private final CgvScreenScheduleService cgvScreenScheduleService;

    @GetMapping("/schedule")
    public List<ScreenScheduleResponseDto> getTheaterSchedule(@RequestBody ScreenScheduleRequestDto requestDto) throws IOException {
        return cgvScreenScheduleService.getTheaterSchedule(
                requestDto.getPlayYMD(),
                requestDto.getMovieIds(),
                requestDto.getTheaterIds()
        );
    }
}
