package com.cinefinder.theater.service;

import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.data.repository.ElasticsearchTheaterRepository;
import com.cinefinder.theater.data.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TheaterSyncService {

    private final TheaterRepository theaterRepository;
    private final Map<String, TheaterCrawlerService> theaterCrawlerServices;
    private final ElasticsearchTheaterRepository elasticsearchTheaterRepository;

    public Map<String, List<Theater>> theaterSyncLogic() {
        Map<String, List<Theater>> theaterInfos = new HashMap<>();

        for (TheaterCrawlerService crawler : theaterCrawlerServices.values()) {

            List<Theater> dbTheaters = theaterRepository.findByBrandName(crawler.getBrandName());

            List<Theater> crawled = crawler.getCrawlData();
//            List<Theater> saved = new ArrayList<>();

            for (Theater theater : crawled) {
                Theater savedTheater = saveIfNotExists(theater);
//                saved.add(savedTheater);
            }

            List<Theater> closedTheaters = dbTheaters.stream()
                    .filter(dbTheater -> crawled.stream().noneMatch(crawledTheater ->
                            crawledTheater.getCode().equals(dbTheater.getCode())))
                    .toList();

            theaterRepository.deleteAll(closedTheaters);
//            for (Theater closedTheater : closedTheaters) {
//                saved.remove(closedTheater);
//            }

            List<Theater> saved = theaterRepository.findByBrandName(crawler.getBrandName());
            crawler.replaceElasticsearchData(saved, elasticsearchTheaterRepository);
            log.info("🗑️ {} 브랜드의 폐업한 영화관 {}개 삭제 완료", crawler.getBrandName(), closedTheaters.size());

            theaterInfos.put(crawler.getBrandName(), saved);
        }

        return theaterInfos;
    }

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
