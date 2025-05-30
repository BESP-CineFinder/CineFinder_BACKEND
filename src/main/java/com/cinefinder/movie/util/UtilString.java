package com.cinefinder.movie.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
public class UtilString {
    private static final List<String> NORMALIZE_STRING_LIST = List.of(
        "[^\\p{IsHangul}\\p{IsAlphabetic}\\p{IsDigit}\\s]",
        "\\s+",
        "극장판"
    );

    public static String getLatestDateString() {
        return getFormattedDateString(1);
    }

    public static String getBeforeDateString() {
        return getFormattedDateString(2);
    }

    private static String getFormattedDateString(int daysOffset) {
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDate calculatedDate = zonedDateTime.minusDays(zonedDateTime.getHour() < 11 ? daysOffset + 1 : daysOffset).toLocalDate();

        return calculatedDate.toString().replaceAll("-", "");
    }

    public static String normalizeMovieKey(String input) {
        for (String normalizeString : NORMALIZE_STRING_LIST) {
            input = input.replaceAll(normalizeString, "");
        }

        return input.toLowerCase().trim();
    }

    public static String normalizeJsonNodeText(String input) {
        if (input.contains("||")) {
            String[] parts = input.split("\\|\\|");
            return parts[parts.length - 1];
        } else {
            return input;
        }
    }
}
