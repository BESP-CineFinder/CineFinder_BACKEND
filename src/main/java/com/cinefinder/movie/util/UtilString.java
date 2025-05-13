package com.cinefinder.movie.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UtilString {
    public static String getLatestDateString() {
        return getFormattedDateString(1);
    }

    public static String getBeforeDateString() {
        return getFormattedDateString(2);
    }

    private static String getFormattedDateString(int daysOffset) {
        LocalDateTime dateTime = LocalDateTime.now();
        LocalDate calculatedDate = dateTime.minusDays(dateTime.getHour() < 11 ? daysOffset + 1 : daysOffset).toLocalDate();

        return calculatedDate.toString().replaceAll("-", "");
    }

    public static String normalizeMovieKey(String input) {
        return input.toLowerCase()
            .replaceAll("[^\\p{IsHangul}\\p{IsAlphabetic}\\p{IsDigit}\\s]", "")
            .replaceAll("\\s+", "")
            .trim();
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
