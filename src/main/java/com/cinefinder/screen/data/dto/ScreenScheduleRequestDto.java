package com.cinefinder.screen.data.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ScreenScheduleRequestDto {
    private String playYMD;
    private Double lat;
    private Double lng;
    private Double distance;
    private List<String> movieNames;
}
