package com.cinefinder.theater.mapper;

import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import com.cinefinder.theater.data.ElasticsearchTheater;
import com.cinefinder.theater.data.Theater;

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
}
