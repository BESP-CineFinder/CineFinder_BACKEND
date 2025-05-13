package com.cinefinder.movie.util;

import com.cinefinder.movie.data.model.BoxOffice;
import com.cinefinder.movie.data.model.MovieDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UtilParse {
    public static List<BoxOffice> extractDailyBoxOfficeInfoList(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // 1. 일간 박스오피스 목록 파싱
            JsonNode root = mapper.readTree(response);
            JsonNode dailyBoxOfficeList = root.path("boxOfficeResult")
                    .path("dailyBoxOfficeList");

            // 2. 요청 및 응답 List 생성
            List<BoxOffice> list = new ArrayList<>();
            for (JsonNode node : dailyBoxOfficeList) {
                String movieNm = node.path("movieNm").asText();

                BoxOffice boxOffice = BoxOffice.builder()
                    .rank(node.path("rank").asText())
                    .movieNm(movieNm)
                    .movieKey(UtilString.normalizeMovieKey(movieNm))
                    .build();

                list.add(boxOffice);
            }

            return list;
        } catch (Exception e) {
            // TODO: JSON 데이터 파싱 실패 시 예외 처리
            throw new RuntimeException("일간 박스오피스 목록 추출 중 오류 발생", e);
        }
    }

    public static List<MovieDetails> extractMovieDetailsList(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<MovieDetails> list = new ArrayList<>();

            // 1. 일간 박스오피스 목록 파싱
            JsonNode result = mapper.readTree(response)
                .path("Data").get(0)
                .path("Result");

            // 2. 응답 결과가 없다면
            if (result.isMissingNode()) {
                log.warn("❌ API 응답 결과가 없음");
                return list;
            }

            // 3. 요청 및 응답 List 생성
            for (JsonNode node : result) {
                String plotText = "";
                String runtime = "";
                String ratingGrade = "";
                String releaseDate = "";

                List<String> directors = new ArrayList<>();
                List<String> actors = new ArrayList<>();
                List<String> vods = new ArrayList<>();

                String title = node.path("title").asText();
                String titleEng = node.path("titleEng").asText();
                String nation = node.path("nation").asText();
                String genre = node.path("genre").asText();
                String posters = node.path("posters").asText();
                String stlls = node.path("stlls").asText();

                JsonNode plotTextNodes = node.path("plots").path("plot");

                for (JsonNode plotTextNode : plotTextNodes) {
                    String lang = plotTextNode.path("plotLang").asText();
                    if ("한국어".equals(lang)) {
                        plotText = plotTextNode.path("plotText").asText();
                        break;
                    }
                }

                JsonNode ratingNodes = node.path("ratings").path("rating");
                for (JsonNode ratingNode : ratingNodes) {
                    runtime = UtilString.normalizeJsonNodeText(ratingNode.path("runtime").asText());
                    ratingGrade = UtilString.normalizeJsonNodeText(ratingNode.path("ratingGrade").asText());
                    releaseDate = UtilString.normalizeJsonNodeText(ratingNode.path("releaseDate").asText())
                            .replaceAll("-", "");
                }

                JsonNode directorsNodes = node.path("directors").path("director");
                for (JsonNode directorNode : directorsNodes) directors.add(directorNode.path("directorNm").asText());

                JsonNode actorsNodes = node.path("actors").path("actor");
                for (JsonNode actorNode : actorsNodes) actors.add(actorNode.path("actorNm").asText());

                JsonNode vodsNode = node.path("vods").path("vod");
                for (JsonNode vod : vodsNode) vods.add(vod.path("vodUrl").asText());

                MovieDetails movieDetails = MovieDetails.builder()
                    .title(title)
                    .titleEng(titleEng)
                    .nation(nation)
                    .genre(genre)
                    .directors(directors)
                    .actors(actors)
                    .plotText(plotText)
                    .runtime(runtime)
                    .ratingGrade(ratingGrade)
                    .releaseDate(releaseDate)
                    .posters(posters)
                    .stlls(stlls)
                    .vods(vods)
                    .build();

                list.add(movieDetails);
            }

            return list;
        } catch (Exception e) {
            // TODO: JSON 데이터 파싱 실패 시 예외 처리
            throw new RuntimeException("영화 상세정보 추출 중 오류 발생", e);
        }
    }
}
