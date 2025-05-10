package com.cinefinder.theater.controller;

import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.service.TheaterCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TheaterCrawlerTestController {

    private final TheaterCrawlerService crawlerService;

    @PostMapping("/import/theater")
    public ResponseEntity<BaseResponse<List<Theater>>> importTheater(@RequestParam String brand) {
        List<Theater> theaters = crawlerService.getCrawlData();
        crawlerService.syncTheaterChanges();
        return ResponseMapper.successOf(ApiStatus._OK, theaters, TheaterCrawlerTestController.class);
    }
}
