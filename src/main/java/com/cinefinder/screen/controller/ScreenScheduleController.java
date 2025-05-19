package com.cinefinder.screen.controller;

import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.screen.data.dto.MovieGroupedScheduleResponseDto;
import com.cinefinder.screen.data.dto.ScreenScheduleRequestDto;
import com.cinefinder.screen.service.ScreenScheduleAggregatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/screen")
public class ScreenScheduleController {

    private final ScreenScheduleAggregatorService screenScheduleAggregatorService;

    @PostMapping("/schedule")
    public ResponseEntity<BaseResponse<List<MovieGroupedScheduleResponseDto>>> getSchedules(@RequestBody ScreenScheduleRequestDto requestDto) {
        return ResponseMapper.successOf(ApiStatus._OK, screenScheduleAggregatorService.getCinemasSchedules(requestDto), ScreenScheduleController.class);
    }
}
