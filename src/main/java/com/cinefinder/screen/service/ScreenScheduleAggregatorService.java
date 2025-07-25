package com.cinefinder.screen.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.dto.MovieResponseDto;
import com.cinefinder.movie.service.MovieService;
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
    private final MovieService movieService;
    private final Map<String, ScreenScheduleService> screenScheduleServices;

    public List<MovieGroupedScheduleResponseDto> getCinemasSchedules(ScreenScheduleRequestDto requestDto) {

        String date = requestDto.getDate().replace("-", "");
        String minTimeStr = (requestDto.getMinTime() == null || requestDto.getMinTime().isEmpty()) ? "00:00" : requestDto.getMinTime();
        String maxTimeStr = (requestDto.getMaxTime() == null || requestDto.getMaxTime().isEmpty()) ? "24:00" : requestDto.getMaxTime();
        double lat = requestDto.getLat();
        double lng = requestDto.getLng();
        double distance = requestDto.getDistance();
        List<Long> movieIds = requestDto.getMovieIds();

        Map<String, List<String>> movieIdsByMultiplex = getMovieIdsByMultiplex(movieIds);
        Map<String, List<String>> theaterIds = theaterService.getNearbyTheaterCodes(lat, lng, distance);

        List<CinemaScheduleApiResponseDto> schedules = getCinemaScheduleApiResponseDtos(screenScheduleServices, date, minTimeStr, maxTimeStr, theaterIds, movieIdsByMultiplex);
        List<MovieGroupedScheduleResponseDto> groupedSchedules = ScreenMapper.toGroupedSchedule(schedules);
        groupedSchedules.sort((a, b) -> {
            int scheduleCountA = a.getSchedule().values().stream()
                    .mapToInt(List::size)
                    .sum();
            int scheduleCountB = b.getSchedule().values().stream()
                    .mapToInt(List::size)
                    .sum();
            return Integer.compare(scheduleCountB, scheduleCountA);
        });

        return groupedSchedules;
    }

    private static List<CinemaScheduleApiResponseDto> getCinemaScheduleApiResponseDtos(Map<String, ScreenScheduleService> screenScheduleServices, String date, String minTimeStr, String maxTimeStr, Map<String, List<String>> theaterIds, Map<String, List<String>> movieIds) {

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime minTime = minTimeStr.equals("24:00") ? LocalTime.MAX : LocalTime.parse(minTimeStr, dateTimeFormatter);
        LocalTime maxTime = maxTimeStr.equals("24:00") ? LocalTime.MAX : LocalTime.parse(maxTimeStr, dateTimeFormatter);

        List<CinemaScheduleApiResponseDto> schedules = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : theaterIds.entrySet()) {
            String brandName = entry.getKey();
            List<String> theaterValues = entry.getValue();
            List<String> movieValues = movieIds.get(brandName);

            for (ScreenScheduleService screenScheduleService : screenScheduleServices.values()) {
                if (screenScheduleService.getBrandName().equals(brandName)) {
                    List<CinemaScheduleApiResponseDto> schedule = screenScheduleService.getTheaterSchedule(date, movieValues, theaterValues).stream()
                            .filter(dto -> {
                                try {
                                    int startTimeHour = Integer.parseInt(dto.getPlayStartTime().split(":")[0]);
                                    LocalTime startTime = startTimeHour >= 24 ? LocalTime.MAX : LocalTime.parse(dto.getPlayStartTime(), dateTimeFormatter);
                                    return (startTime.equals(minTime) || startTime.isAfter(minTime)) &&
                                            (startTime.equals(maxTime) || startTime.isBefore(maxTime));
                                } catch (NullPointerException | ArrayIndexOutOfBoundsException | NumberFormatException e) {
                                    throw new CustomException(ApiStatus._INTERNAL_SERVER_ERROR, "시간 형식이 이상한 상영 정보가 있습니다.\n startTime: " + dto.getPlayStartTime() + " endTime: " + dto.getPlayEndTime());
                                }
                            })
                            .toList();

                    schedules.addAll(schedule);
                }
            }
        }
        return schedules;
    }

    private Map<String, List<String>> getMovieIdsByMultiplex(List<Long> movieIds) {
        Map<String, List<String>> movieIdsByMultiplex = new HashMap<>() {{
            put(cgvBrandName, new ArrayList<>());
            put(lotteBrandName, new ArrayList<>());
            put(megaBrandName, new ArrayList<>());
        }};

        for (Long movieId : movieIds) {
            MovieResponseDto movieDetail = movieService.getMovieDetailsByMovieId(movieId);
            String cgvCode = movieDetail.getCgvCode();
            String lotteCode = movieDetail.getLotteCinemaCode();
            String megaCode = movieDetail.getMegaBoxCode();

            if (cgvCode != null) {
                movieIdsByMultiplex.get(cgvBrandName).add(cgvCode);
            } else {
                movieIdsByMultiplex.get(cgvBrandName).add("NONE");
            }
            if (lotteCode != null) {
                movieIdsByMultiplex.get(lotteBrandName).add(lotteCode);
            } else {
                movieIdsByMultiplex.get(lotteBrandName).add("NONE");
            }
            if (megaCode != null) {
                movieIdsByMultiplex.get(megaBrandName).add(megaCode);
            } else {
                movieIdsByMultiplex.get(megaBrandName).add("NONE");
            }
        }

        return movieIdsByMultiplex;
    }
}
