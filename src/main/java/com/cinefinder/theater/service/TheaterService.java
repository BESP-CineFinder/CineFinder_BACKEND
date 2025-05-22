package com.cinefinder.theater.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.data.repository.TheaterRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

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
public class TheaterService {

	private final RedissonClient redissonClient;
	private final TheaterRepository theaterRepository;
	private final ElasticsearchClient elasticsearchClient;
	private final TheaterDbSyncService theaterDbSyncService;

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
		RLock lock = redissonClient.getLock("theater-sync-lock");
		log.info("🔒[영화관 데이터 갱신] 락 시도중...");
		boolean isLocked = false;

		try {
			isLocked = lock.tryLock(10, 300, TimeUnit.SECONDS);

			if (!isLocked) {
				log.info("🔒[영화관 데이터 갱신] 다른 서버에서 영화관을 이미 갱신하고 있어서 초기화를 스킵합니다.");
				throw new CustomException(ApiStatus._SIMULTANEOUS_USE_ERROR);
			}

			log.info("🔒[영화관 데이터 갱신] 락 획득 성공!");
			return theaterDbSyncService.theaterSyncLogic();

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("동기화 중단됨", e);
		} finally {
			if (isLocked) {
				lock.unlock();
			}
		}
	}


}
