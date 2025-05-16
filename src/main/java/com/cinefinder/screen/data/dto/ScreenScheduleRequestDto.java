package com.cinefinder.screen.data.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ScreenScheduleRequestDto {
    private String date; // "2023-10-01"
    private String minTime; // "10:00"
    private String maxTime; // "18:00"
    private Double lat;
    private Double lng;
    private Double distance;
    private List<String> movieNames;
}
