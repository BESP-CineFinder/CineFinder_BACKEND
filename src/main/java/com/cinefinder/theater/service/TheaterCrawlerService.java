package com.cinefinder.theater.service;

import com.cinefinder.theater.data.Theater;

import java.util.List;

public interface TheaterCrawlerService {
    List<Theater> getCrawlData();
    void syncRecentTheater(List<Theater> theaters);
}
