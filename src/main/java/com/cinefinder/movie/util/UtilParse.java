package com.cinefinder.movie.util;

import com.cinefinder.movie.data.model.BoxOffice;
import com.cinefinder.movie.data.model.MovieDetails;
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
    public static List<BoxOffice> extractDailyBoxOfficeInfoList(String response) throws JsonProcessingException {
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
    }

    public static List<MovieDetails> extractMovieDetailsList(String response, String movieKey) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<MovieDetails> list = new ArrayList<>();

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

            MovieDetails movieDetails = MovieDetails.builder()
                .movieKey(movieKey)
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

            list.add(movieDetails);
        }

        return list;
    }

    public static MovieDetails extractDaumMovieDetails(String response) {
        Document doc = Jsoup.parse(response);
        Elements dts = doc.select("dt");

        if (dts.isEmpty()) return null;

        MovieDetails movieDetails = new MovieDetails();
        for (Element dt : dts) {
            Element dd = dt.nextElementSibling();
            if (dd == null) continue;

            switch (dt.text()) {
                case "국가" -> movieDetails.updateNation(dd.text());
                case "장르" -> movieDetails.updateGenre(dd.text());
                case "등급" -> movieDetails.updateRatingGrade(dd.text());
                case "시간" -> movieDetails.updateRuntime(dd.text().replace("분", ""));
                case "개봉" -> movieDetails.updateReleaseDate(dd.text().replace(".", ""));
            }
        }

        if (StringUtil.isNullOrEmpty(movieDetails.getPlotText())) {
            Element summary = doc.selectFirst("c-summary");
            if (summary != null) movieDetails.updatePlotText(summary.text());
        }

        if (StringUtil.isNullOrEmpty(movieDetails.getPosters())) {
            Element cDocContent = doc.selectFirst("c-doc-content");
            if (cDocContent != null) {
                Element cThumb = cDocContent.selectFirst("c-thumb");
                if (cThumb != null) movieDetails.updatePosters(cThumb.attr("data-original-src"));
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

            movieDetails.updateVods(String.join("|", vodList));
            movieDetails.updateStlls(String.join("|", stllList));
        }

        return movieDetails;
    }

    public static List<MovieDetails> extractCgvMovieList(String response) {
        if (response == null) return new ArrayList<>();

        Document doc = Jsoup.parse(response);
        Elements movieItems = doc.select("div.mm_list_item");

        List<MovieDetails> movieDetailsList = new ArrayList<>();
        for (Element movie : movieItems) {
            MovieDetails movieDetails = new MovieDetails();

            movieDetails.updateTitle(movie.selectFirst("div.mm_list_item strong.tit").text());

            Element button = movie.selectFirst("a.btn_reserve");
            String onclick = button.attr("onclick");
            String[] argsArray = onclick.split("'");
            if (argsArray.length >= 4) {
                movieDetails.updateCgvCode(argsArray[3]);
            }

            movieDetailsList.add(movieDetails);
        }

        return movieDetailsList;
    }

    public static List<MovieDetails> extractMegaBoxMovieList(String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode curationBannerListNodes = mapper.readTree(response).path("movieList");

        List<MovieDetails> movieDetailsList = new ArrayList<>();
        for (JsonNode node : curationBannerListNodes) {
            MovieDetails movieDetails = new MovieDetails();

            movieDetails.updateTitle(decodeUnicode(node.path("movieNm").asText()));
            movieDetails.updateMegaBoxCode(node.path("movieNo").asText());

            movieDetailsList.add(movieDetails);
        }

        return movieDetailsList;
    }

    public static List<MovieDetails> extractLotteCinemaMovieList(String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode items = mapper.readTree(response)
            .path("Movies")
            .path("Items");

        List<MovieDetails> movieDetailsList = new ArrayList<>();
        for (JsonNode node : items) {
            MovieDetails movieDetails = new MovieDetails();

            movieDetails.updateTitle(decodeUnicode(node.path("MovieNameKR").asText()));
            movieDetails.updateLotteCinemaCode(node.path("RepresentationMovieCode").asText());

            movieDetailsList.add(movieDetails);
        }

        return movieDetailsList;
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
