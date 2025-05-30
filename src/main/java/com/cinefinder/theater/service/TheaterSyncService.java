package com.cinefinder.theater.service;

import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.data.repository.ElasticsearchTheaterRepository;
import com.cinefinder.theater.data.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

            String brandName = crawler.getBrandName();
            List<Theater> dbTheaters = theaterRepository.findByBrandName(brandName);
            List<Theater> crawled = crawler.getCrawlData();

            for (Theater theater : crawled) {
                saveIfNotExists(theater);
            }

            deleteIfClosed(brandName, dbTheaters, crawled);

            List<Theater> savedTheaters = saveToElasticsearch(crawler);

            theaterInfos.put(brandName, savedTheaters);
        }

        log.info("🏢[영화관 데이터 갱신 - 완료] 현재 영화관 총 {}개 처리 완료", theaterInfos.values().stream().mapToInt(List::size).sum());
        return theaterInfos;
    }

    private void saveIfNotExists(Theater newTheater) {
        theaterRepository
                .findByBrandNameAndCode(newTheater.getBrand().getName(), newTheater.getCode())
                .map(existingTheater -> {
                    log.debug("🏢[영화관 데이터 갱신 - 중복] 브랜드: {}, 영화관: {}, 코드: {} 이미 존재", existingTheater.getBrand().getName(), existingTheater.getName(), existingTheater.getCode());
                    return existingTheater;
                })
                .orElseGet(() -> {
                    log.info("🏢[영화관 데이터 갱신 - 신규] 브랜드: {}, 영화관: {}, 코드: {} 저장 완료", newTheater.getBrand().getName(), newTheater.getName(), newTheater.getCode());
                    return theaterRepository.save(newTheater);
                });
    }

    private void deleteIfClosed(String brandName, List<Theater> dbData, List<Theater> crawledData) {
        List<Theater> closedTheaters = dbData.stream()
                .filter(dbTheater -> crawledData.stream().noneMatch(crawledTheater ->
                        crawledTheater.getCode().equals(dbTheater.getCode())))
                .toList();
        theaterRepository.deleteAll(closedTheaters);
        log.info("🏢[영화관 데이터 갱신 - 폐업] {} 브랜드의 폐업한 영화관 {}개 삭제 완료", brandName, closedTheaters.size());
    }

    private List<Theater> saveToElasticsearch(TheaterCrawlerService theaterCrawlerService) {
        List<Theater> saved = theaterRepository.findByBrandName(theaterCrawlerService.getBrandName());
        theaterCrawlerService.replaceElasticsearchData(saved, elasticsearchTheaterRepository);
        log.info("🏢[영화관 데이터 갱신 - ES] {} 브랜드의 영화관 {}개 저장 완료", theaterCrawlerService.getBrandName(), saved.size());
        return saved;
    }
}
