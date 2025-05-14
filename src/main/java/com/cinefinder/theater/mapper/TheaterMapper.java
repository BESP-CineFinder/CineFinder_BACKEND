package com.cinefinder.theater.mapper;

import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import com.cinefinder.theater.data.ElasticsearchTheater;
import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.data.dto.SimplifiedTheaterDto;

public class TheaterMapper {

	public static ElasticsearchTheater toElasticsearchDocument(Theater theater) {
		return ElasticsearchTheater.builder()
			.id(theater.getBrand().getName() + "_" + theater.getCode())
			.brand(theater.getBrand().getName())
			.theaterId(theater.getCode())
			.location(new GeoPoint(
				theater.getLatitude().doubleValue(),
				theater.getLongitude().doubleValue()))
			.build();
	}
  
  public static SimplifiedTheaterDto toSimplifiedTheaterDto(Theater theater) {
    return new SimplifiedTheaterDto(
        theater.getId(),
        theater.getCode(),
        theater.getName()
    );
  }
}