package com.cinefinder.theater.controller;

import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.service.TheaterRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/theater")
public class TheaterCrawlerController {

    private final TheaterRoutingService theaterRoutingService;

    @PostMapping("/sync")
    public ResponseEntity<BaseResponse<Map<String, List<Theater>>>> importTheater() {
        return ResponseMapper.successOf(ApiStatus._OK, theaterRoutingService.getTheaterInfosAfterSync(), TheaterCrawlerController.class);
    }
}
