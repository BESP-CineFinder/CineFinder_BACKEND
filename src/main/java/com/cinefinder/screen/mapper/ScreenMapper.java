package com.cinefinder.screen.mapper;

import com.cinefinder.movie.data.SimplifiedMovieDto;
import com.cinefinder.screen.data.dto.CinemaScheduleApiResponseDto;
import com.cinefinder.screen.data.dto.MovieGroupedScheduleResponseDto;
import com.cinefinder.screen.data.dto.MovieScheduleDetailDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScreenMapper {

    public static List<MovieGroupedScheduleResponseDto> toGroupedSchedule(List<CinemaScheduleApiResponseDto> apiResponseList) {
        Map<Long, List<CinemaScheduleApiResponseDto>> groupedByMovie = apiResponseList.stream()
                .collect(Collectors.groupingBy(dto -> dto.getMovie().getId()));

        List<MovieGroupedScheduleResponseDto> result = new ArrayList<>();

        for (Map.Entry<Long, List<CinemaScheduleApiResponseDto>> entry : groupedByMovie.entrySet()) {
            List<CinemaScheduleApiResponseDto> schedules = entry.getValue();

            if (schedules.isEmpty()) continue;

            SimplifiedMovieDto movie = schedules.getFirst().getMovie();
            Map<String, List<MovieScheduleDetailDto>> scheduleMap = new HashMap<>();

            for (CinemaScheduleApiResponseDto dto : schedules) {
                String brandName = dto.getBrand().getName();

                MovieScheduleDetailDto detailDto = new MovieScheduleDetailDto(
                        dto.getTheater(),
                        dto.getFilmNm(),
                        dto.getScreenNm(),
                        dto.getPlayYmd(),
                        dto.getPlayStartTime(),
                        dto.getPlayEndTime(),
                        Integer.parseInt(dto.getSeatRemainCnt()),
                        Integer.parseInt(dto.getSeatCapacityCnt())
                );

                scheduleMap.computeIfAbsent(brandName, k -> new ArrayList<>()).add(detailDto);
            }

            MovieGroupedScheduleResponseDto groupedDto = new MovieGroupedScheduleResponseDto(
                    movie.getId(),
                    movie.getTitleKor(),
                    movie.getPoster(),
                    scheduleMap
            );

            result.add(groupedDto);
        }

        return result;
    }
}
