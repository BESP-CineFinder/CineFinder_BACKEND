package com.cinefinder.screen.service;

import com.cinefinder.movie.data.model.MovieDetails;
import com.cinefinder.movie.service.MovieDetailService;
import com.cinefinder.screen.data.dto.MovieGroupedScheduleResponseDto;
import com.cinefinder.screen.data.dto.ScreenScheduleRequestDto;
import com.cinefinder.screen.data.dto.CinemaScheduleApiResponseDto;
import com.cinefinder.screen.mapper.ScreenMapper;
import com.cinefinder.theater.service.TheaterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenScheduleAggregatorService {

    @Value("${movie.cgv.name}")
    private String cgvBrandName;

    @Value("${movie.lotte.name}")
    private String lotteBrandName;

    @Value("${movie.mega.name}")
    private String megaBrandName;

    private final TheaterService theaterService;
    private final MovieDetailService movieDetailService;
    private final Map<String, ScreenScheduleService> screenScheduleServices;

    public List<MovieGroupedScheduleResponseDto> getCinemasSchedules(ScreenScheduleRequestDto requestDto) {

        String date = requestDto.getDate().replace("-", "");
        String minTimeStr = requestDto.getMinTime();
        String maxTimeStr = requestDto.getMaxTime();
        double lat = requestDto.getLat();
        double lng = requestDto.getLng();
        double distance = requestDto.getDistance();
        List<String> movieNames = requestDto.getMovieNames();

        Map<String, List<String>> movieIds = getMovieIds(movieNames);
        Map<String, List<String>> theaterIds = theaterService.getTheaterInfos(lat, lng, distance);

        List<CinemaScheduleApiResponseDto> schedules = getCinemaScheduleApiResponseDtos(screenScheduleServices, date, minTimeStr, maxTimeStr, theaterIds, movieIds);

        return ScreenMapper.toGroupedSchedule(schedules);
    }

    private static List<CinemaScheduleApiResponseDto> getCinemaScheduleApiResponseDtos(Map<String, ScreenScheduleService> screenScheduleServices, String date, String minTimeStr, String maxTimeStr, Map<String, List<String>> theaterIds, Map<String, List<String>> movieIds) {

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        List<CinemaScheduleApiResponseDto> schedules = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : theaterIds.entrySet()) {
            String brandName = entry.getKey();
            List<String> theaterValues = entry.getValue();
            List<String> movieValues = movieIds.get(brandName);

            for (ScreenScheduleService screenScheduleService : screenScheduleServices.values()) {
                if (screenScheduleService.getBrandName().equals(brandName)) {
                    List<CinemaScheduleApiResponseDto> schedule = screenScheduleService.getTheaterSchedule(date, movieValues, theaterValues).stream()
                            .filter(dto -> {
                                LocalTime startTime = LocalTime.parse(dto.getPlayStartTime(), dateTimeFormatter);
                                LocalTime minTime = LocalTime.parse(minTimeStr, dateTimeFormatter);
                                LocalTime maxTime = LocalTime.parse(maxTimeStr, dateTimeFormatter);
                                return (startTime.equals(minTime) || startTime.isAfter(minTime)) &&
                                        (startTime.equals(maxTime) || startTime.isBefore(maxTime));
                            })
                            .toList();

                    schedules.addAll(schedule);
                }
            }
        }
        return schedules;
    }

    private Map<String, List<String>> getMovieIds(List<String> movieNames) {
        Map<String, List<String>> movieIds = new HashMap<>() {{
            put(cgvBrandName, new ArrayList<>());
            put(lotteBrandName, new ArrayList<>());
            put(megaBrandName, new ArrayList<>());
        }};

        for (String movieName : movieNames) {
            MovieDetails movieDetail = movieDetailService.getMovieDetails(movieName);
            String cgvCode = movieDetail.getCgvCode();
            String lotteCode = movieDetail.getLotteCinemaCode();
            String megaCode = movieDetail.getMegaBoxCode();

            if (cgvCode != null) {
                movieIds.get(cgvBrandName).add(cgvCode);
            }
            if (lotteCode != null) {
                movieIds.get(lotteBrandName).add(lotteCode);
            }
            if (megaCode != null) {
                movieIds.get(megaBrandName).add(megaCode);
            }
        }

        return movieIds;
    }
}
