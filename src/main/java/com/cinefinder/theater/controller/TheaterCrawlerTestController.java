package com.cinefinder.theater.controller;

import com.cinefinder.global.mapper.ResponseMapper;
import com.cinefinder.global.response.BaseResponse;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.service.TheaterCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/test")
public class TheaterCrawlerTestController {

    private final ApplicationContext applicationContext;

    @PostMapping("/import/theater")
    public ResponseEntity<BaseResponse<List<Theater>>> importTheater(@RequestParam String brand) {
        Map<String, TheaterCrawlerService> theaterCrawlerServices = applicationContext.getBeansOfType(TheaterCrawlerService.class);
        System.out.println(theaterCrawlerServices);

        TheaterCrawlerService theaterCrawlerService = theaterCrawlerServices.get(brand+"TheaterCrawler");

        List<Theater> theaters = theaterCrawlerService.getCrawlData();
        theaterCrawlerService.syncRecentTheater(theaters);
        return ResponseMapper.successOf(ApiStatus._OK, theaters, TheaterCrawlerTestController.class);
    }
}
