package com.cinefinder.screen.service;

import com.cinefinder.movie.data.model.MovieDetails;
import com.cinefinder.movie.service.MovieDetailService;
import com.cinefinder.screen.data.dto.ScreenScheduleRequestDto;
import com.cinefinder.screen.data.dto.ScreenScheduleResponseDto;
import com.cinefinder.theater.service.TheaterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

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
    private final ApplicationContext applicationContext;

    public List<ScreenScheduleResponseDto> getCinemasSchedules(ScreenScheduleRequestDto requestDto) {

        String playYMD = requestDto.getPlayYMD();
        double lat = requestDto.getLat();
        double lng = requestDto.getLng();
        double distance = requestDto.getDistance();
        List<String> movieNames = requestDto.getMovieNames();

        Map<String, List<String>> movieIds = getMovieIds(movieNames);
        Map<String, List<String>> theaterIds = theaterService.getTheaterInfos(lat, lng, distance);
        Collection<ScreenScheduleService> screenScheduleServices = applicationContext.getBeansOfType(ScreenScheduleService.class).values();

        List<ScreenScheduleResponseDto> schedules = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : theaterIds.entrySet()) {
            String brandName = entry.getKey();
            List<String> theaterValues = entry.getValue();
            List<String> movieValues = movieIds.get(brandName);

            for (ScreenScheduleService screenScheduleService : screenScheduleServices) {
                if (screenScheduleService.getBrandName().equals(brandName)) {
                    List<ScreenScheduleResponseDto> schedule = screenScheduleService.getTheaterSchedule(playYMD, movieValues, theaterValues);
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
