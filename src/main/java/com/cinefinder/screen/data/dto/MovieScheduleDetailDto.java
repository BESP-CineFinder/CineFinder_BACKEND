package com.cinefinder.screen.data.dto;

import com.cinefinder.theater.data.dto.SimplifiedTheaterDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MovieScheduleDetailDto {
    private String multiplexMovieCode;
    private SimplifiedTheaterDto theater;
    private String film;
    private String screen;
    private String date;
    private String start;
    private String end;
    private int remainingSeats;
    private int totalSeats;
}
