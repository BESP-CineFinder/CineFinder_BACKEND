package com.cinefinder.theater.data.repository;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.cinefinder.theater.data.ElasticsearchTheater;

@Repository
public interface ElasticsearchTheaterRepository extends ElasticsearchRepository<ElasticsearchTheater, String> {

	List<ElasticsearchTheater> findByBrand(String brand);
}
