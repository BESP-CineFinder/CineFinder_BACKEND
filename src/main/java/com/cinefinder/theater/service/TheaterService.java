package com.cinefinder.theater.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.data.repository.ElasticsearchTheaterRepository;
import com.cinefinder.theater.data.repository.TheaterRepository;
import lombok.extern.slf4j.Slf4j;
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




@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TheaterService {

	private final TheaterRepository theaterRepository;
	private final Map<String, TheaterCrawlerService> theaterCrawlerServices;
	private final ElasticsearchClient elasticsearchClient;
	private final ElasticsearchTheaterRepository elasticsearchTheaterRepository;

	public Theater getTheaterInfo(String brand, String theaterId) {
		return theaterRepository.findByBrandNameAndCode(brand, theaterId)
				.orElseThrow(() -> new CustomException(ApiStatus._NOT_FOUND));
	}

	public Map<String,List<String>> getNearbyTheaterCodes(Double lat, Double lon, Double distance) {
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

		for (TheaterCrawlerService crawler : theaterCrawlerServices.values()) {

			List<Theater> dbTheaters = theaterRepository.findByBrandName(crawler.getBrandName());

			List<Theater> crawled = crawler.getCrawlData();
			List<Theater> saved = new ArrayList<>();

			for (Theater theater : crawled) {
				Theater savedTheater = saveIfNotExists(theater);
				saved.add(savedTheater);
			}

			List<Theater> closedTheaters = dbTheaters.stream()
					.filter(dbTheater -> crawled.stream().noneMatch(crawledTheater ->
							crawledTheater.getCode().equals(dbTheater.getCode())))
					.toList();

			theaterRepository.deleteAll(closedTheaters);
			crawler.replaceElasticsearchData(saved, elasticsearchTheaterRepository);
			log.info("🗑️ {} 브랜드의 폐업한 영화관 {}개 삭제 완료", crawler.getBrandName(), closedTheaters.size());

			theaterInfos.put(crawler.getBrandName(), saved);
		}

		return theaterInfos;
	}

	@Transactional
	public Theater saveIfNotExists(Theater newTheater) {
		return theaterRepository
				.findByBrandNameAndCode(newTheater.getBrand().getName(), newTheater.getCode())
				.map(existingTheater -> {
					log.info("⚠️[중복 영화관] 브랜드: {}, 영화관: {}, 코드: {} 이미 존재!", existingTheater.getBrand().getName(), existingTheater.getName(), existingTheater.getCode());
					return existingTheater;
				})
				.orElseGet(() -> {
					log.info("🆕[신규 영화관] 브랜드: {}, 영화관: {}, 코드: {} 저장 완료!", newTheater.getBrand().getName(), newTheater.getName(), newTheater.getCode());
					return theaterRepository.save(newTheater);
				});
	}
}
