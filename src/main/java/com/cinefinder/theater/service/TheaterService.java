package com.cinefinder.theater.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.data.repository.TheaterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.theater.data.ElasticsearchTheater;
import com.cinefinder.global.util.statuscode.ApiStatus;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.DistanceUnit;
import co.elastic.clients.elasticsearch._types.LatLonGeoLocation;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;




@Service
@RequiredArgsConstructor
@Transactional
public class TheaterService {

	private final TheaterRepository theaterRepository;
	private final Map<String, TheaterCrawlerService> theaterCrawlerServices;
	private final ElasticsearchClient elasticsearchClient;

	public Theater getTheaterInfo(String brand, String theaterId) {
		return theaterRepository.findByBrandNameAndCode(brand, theaterId);
	}

	public Map<String,List<String>> getTheaterInfos(Double lat, Double lon, Double distance) {
		Map<String,List<String>> results = new HashMap<>();

		try {
			LatLonGeoLocation userLocation = new LatLonGeoLocation.Builder()
				.lat(lat)
				.lon(lon)
				.build();

			SearchRequest searchRequest = SearchRequest.of(s -> s
				.index("theater")
				.query(q -> q
					.bool(b -> b
						.filter(f -> f
							.geoDistance(gd -> gd
								.field("location")
								.distance(distance + "km")
								.location(loc -> loc
									.latlon(userLocation)
								)
							)
						)
					)
				)
				.sort(sort -> sort
					.geoDistance(g -> g
						.field("location")
						.location(loc -> loc
							.latlon(userLocation))
						.unit(DistanceUnit.Kilometers)
						.order(SortOrder.Asc)
					)
				)
			);

			SearchResponse<ElasticsearchTheater> response = elasticsearchClient.search(searchRequest, ElasticsearchTheater.class);

			for (Hit<ElasticsearchTheater> hit : response.hits().hits()) {
				ElasticsearchTheater theater = hit.source();
				if (theater != null) {
					String brand = theater.getBrand();
					String theaterId = theater.getTheaterId();

					results.computeIfAbsent(brand, k -> new ArrayList<>()).add(theaterId);
				}
			}
		} catch (Exception e) {
			throw new CustomException(ApiStatus._READ_FAIL);
		}
		return results;
	}

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
