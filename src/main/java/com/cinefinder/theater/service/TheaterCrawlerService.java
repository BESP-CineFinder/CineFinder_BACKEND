package com.cinefinder.theater.service;

import com.cinefinder.theater.data.ElasticsearchTheater;
import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.data.repository.ElasticsearchTheaterRepository;
import com.cinefinder.theater.mapper.TheaterMapper;

import java.util.List;

public interface TheaterCrawlerService {
    List<Theater> getCrawlData();
    String getBrandName();

    default List<ElasticsearchTheater> returnToElasticsearch(List<Theater> theaters, ElasticsearchTheaterRepository repo) {
        return theaters.stream()
            .map(TheaterMapper::toElasticsearchDocument)
            .toList();
    }

    default void replaceElasticsearchData(List<Theater> theaters, ElasticsearchTheaterRepository repo) {
        String brandName = getBrandName();

        List<ElasticsearchTheater> oldDocs = repo.findByBrand(brandName);
        List<String> oldIds = oldDocs.stream().map(ElasticsearchTheater::getId).toList();

        repo.deleteAllById(oldIds);
        repo.saveAll(returnToElasticsearch(theaters, repo));
    }
}
