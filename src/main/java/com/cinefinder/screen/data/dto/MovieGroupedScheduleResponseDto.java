package com.cinefinder.screen.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class MovieGroupedScheduleResponseDto {
    private Long id;
    private String name;
    private String poster;
    private Map<String, List<MovieScheduleDetailDto>> schedule;
}
