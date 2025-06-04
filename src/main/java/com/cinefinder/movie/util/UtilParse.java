package com.cinefinder.movie.util;

import com.cinefinder.movie.data.dto.BoxOfficeResponseDto;
import com.cinefinder.movie.data.dto.MovieResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
public class UtilParse {
    public static List<BoxOfficeResponseDto> extractDailyBoxOfficeInfoList(String response) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        // 1. 일간 박스오피스 목록 파싱
        JsonNode root = mapper.readTree(response);
        JsonNode dailyBoxOfficeList = root.path("boxOfficeResult")
                .path("dailyBoxOfficeList");

        // 2. 요청 및 응답 List 생성
        List<BoxOfficeResponseDto> list = new ArrayList<>();
        for (JsonNode node : dailyBoxOfficeList) {
            String movieNm = node.path("movieNm").asText();

            BoxOfficeResponseDto boxOfficeResponseDto = BoxOfficeResponseDto.builder()
                .rank(node.path("rank").asText())
                .movieNm(movieNm)
                .movieKey(UtilString.normalizeMovieKey(movieNm))
                .build();

            list.add(boxOfficeResponseDto);
        }

        return list;
    }

    public static List<MovieResponseDto> extractMovieDetailsList(String response, String movieKey) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<MovieResponseDto> list = new ArrayList<>();

        // 1. 일간 박스오피스 목록 파싱
        JsonNode result = mapper.readTree(response)
            .path("Data").get(0)
            .path("Result");

        // 2. 응답 결과가 없다면
        if (result.isMissingNode()) return list;

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
            if (actors.size() > 5) actors = actors.subList(0, 5);

            JsonNode vodsNode = node.path("vods").path("vod");
            for (JsonNode vod : vodsNode) vods.add(vod.path("vodUrl").asText());

            MovieResponseDto movieResponseDto = MovieResponseDto.builder()
                .title(title)
                .titleEng(titleEng)
                .nation(nation)
                .genre(genre)
                .plotText(plotText)
                .runtime(runtime)
                .ratingGrade(ratingGrade)
                .releaseDate(releaseDate)
                .posters(posters)
                .stlls(stlls)
                .directors(String.join("|", directors))
                .actors(String.join("|", actors))
                .vods(String.join("|", vods))
                .build();

            list.add(movieResponseDto);
        }

        return list;
    }

    public static MovieResponseDto extractDaumMovieDetails(String response) {
        Document doc = Jsoup.parse(response);
        Elements dts = doc.select("dt");

        if (dts.isEmpty()) return null;

        MovieResponseDto movieResponseDto = new MovieResponseDto();
        for (Element dt : dts) {
            Element dd = dt.nextElementSibling();
            if (dd == null) continue;

            switch (dt.text()) {
                case "국가" -> movieResponseDto.updateNation(dd.text());
                case "장르" -> movieResponseDto.updateGenre(dd.text());
                case "등급" -> movieResponseDto.updateRatingGrade(dd.text());
                case "시간" -> movieResponseDto.updateRuntime(dd.text().replace("분", ""));
                case "개봉" -> movieResponseDto.updateReleaseDate(dd.text().replace(".", ""));
            }
        }

        if (StringUtil.isNullOrEmpty(movieResponseDto.getPlotText())) {
            Element summary = doc.selectFirst("c-summary");
            if (summary != null) movieResponseDto.updatePlotText(summary.text());
        }

        if (StringUtil.isNullOrEmpty(movieResponseDto.getPosters())) {
            Element cDocContent = doc.selectFirst("c-doc-content");
            if (cDocContent != null) {
                Element cThumb = cDocContent.selectFirst("c-thumb");
                if (cThumb != null) movieResponseDto.updatePosters(cThumb.attr("data-original-src"));
            }
        }

        Elements tabs = doc.select("c-tab-pannel");
        if (!tabs.isEmpty()) {
            List<String> vodList = new ArrayList<>();
            List<String> stllList = new ArrayList<>();
            for (Element tab : tabs) {
                Element tabElement = tab.selectFirst("strong");
                if (tabElement == null) continue;

                String tabText = tabElement.text();
                switch (tabText) {
                    case "영상" -> vodList.add(tab.select("c-thumb").attr("data-href"));
                    case "포토" -> stllList.add(tab.select("c-thumb").attr("data-original-src"));
                }
            }

            movieResponseDto.updateVods(String.join("|", vodList));
            movieResponseDto.updateStlls(String.join("|", stllList));
        }

        return movieResponseDto;
    }

    public static List<MovieResponseDto> extractCgvMovieList(String response) {
        if (response == null) return new ArrayList<>();

        Document doc = Jsoup.parse(response);
        Elements movieItems = doc.select("div.mm_list_item");

        List<MovieResponseDto> movieResponseDtoList = new ArrayList<>();
        for (Element movie : movieItems) {
            MovieResponseDto movieResponseDto = new MovieResponseDto();

            movieResponseDto.updateTitle(movie.selectFirst("div.mm_list_item strong.tit").text());

            Element button = movie.selectFirst("a.btn_reserve");
            String onclick = button.attr("onclick");
            String[] argsArray = onclick.split("'");
            if (argsArray.length >= 4) {
                movieResponseDto.updateCgvCode(argsArray[3]);
            }

            movieResponseDtoList.add(movieResponseDto);
        }

        return movieResponseDtoList;
    }

    public static List<MovieResponseDto> extractMegaBoxMovieList(String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode curationBannerListNodes = mapper.readTree(response).path("movieList");

        List<MovieResponseDto> movieResponseDtoList = new ArrayList<>();
        for (JsonNode node : curationBannerListNodes) {
            MovieResponseDto movieResponseDto = new MovieResponseDto();

            movieResponseDto.updateTitle(decodeUnicode(node.path("movieNm").asText()));
            movieResponseDto.updateMegaBoxCode(node.path("movieNo").asText());

            movieResponseDtoList.add(movieResponseDto);
        }

        return movieResponseDtoList;
    }

    public static List<MovieResponseDto> extractLotteCinemaMovieList(String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode items = mapper.readTree(response)
            .path("Movies")
            .path("Items");

        List<MovieResponseDto> movieResponseDtoList = new ArrayList<>();
        for (JsonNode node : items) {
            MovieResponseDto movieResponseDto = new MovieResponseDto();

            movieResponseDto.updateTitle(decodeUnicode(node.path("MovieNameKR").asText()));
            movieResponseDto.updateLotteCinemaCode(node.path("RepresentationMovieCode").asText());

            movieResponseDtoList.add(movieResponseDto);
        }

        return movieResponseDtoList;
    }

    private static String decodeUnicode(String input) throws IOException {
        input = input.replaceAll("&amp;", "&")
            .replaceAll("&lt;", "<")
            .replaceAll("&gt;", ">")
            .replaceAll("&quot;", "\"")
            .replaceAll("&apos;", "'")
            .replaceAll("&nbsp;", " ");

        Properties prop = new Properties();
        prop.load(new java.io.StringReader("key=" + input));
        return prop.getProperty("key");
    }
}
