package com.cinefinder.theater.service;

import com.cinefinder.theater.data.Theater;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TheaterRoutingService {

    private final Map<String, TheaterCrawlerService> theaterCrawlerServices;

    public Map<String, List<Theater>> getTheaterInfosAfterSync() {
        Map<String, List<Theater>> theaterInfos = new HashMap<>();
        for (TheaterCrawlerService theaterCrawlerService : theaterCrawlerServices.values()) {
            List<Theater> theaters = theaterCrawlerService.getCrawlData();
            theaterCrawlerService.syncRecentTheater(theaters);
            theaterInfos.put(theaterCrawlerService.getBrandName(), theaters);
        }

        return theaterInfos;
    }
}
