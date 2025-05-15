package com.cinefinder.screen.data.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ScreenScheduleRequestDto {
    private String playYMD;
    private List<String> movieIds;
    private List<String> theaterIds;
}
