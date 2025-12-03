package com.example.habitor.utils;

import com.example.habitor.model.RepeatPattern;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for formatting and parsing repeat patterns.
 * Converts between RepeatPattern enum/days configuration and human-readable strings.
 */
public class RepeatPatternFormatter {

    private static final String[] DAY_NAMES = {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    private static final String[] DAY_ABBREVIATIONS = {
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    // Format prefixes for parsing
    private static final String PREFIX_DAILY = "Daily";
    private static final String PREFIX_WEEKLY = "Weekly: ";
    private static final String PREFIX_EVERY = "Every ";
    private static final String SUFFIX_DAYS = " days";

    /**
     * Format a repeat pattern to a human-readable string.
     *
     * @param pattern The repeat pattern type
     * @param repeatDaysJson JSON array string of day indices (0=Sunday, 1=Monday, etc.) for WEEKLY
     * @param customIntervalDays Interval in days for CUSTOM pattern
     * @return Human-readable string representation
     */
    public static String formatToReadable(RepeatPattern pattern, String repeatDaysJson, int customIntervalDays) {
        if (pattern == null) {
            pattern = RepeatPattern.DAILY;
        }

        switch (pattern) {
            case DAILY:
                return PREFIX_DAILY;

            case WEEKLY:
                return formatWeeklyPattern(repeatDaysJson);

            case CUSTOM:
                return formatCustomPattern(customIntervalDays);

            default:
                return PREFIX_DAILY;
        }
    }

    /**
     * Parse a human-readable string back to RepeatPattern and configuration.
     * Returns a ParsedPattern object containing the pattern type and configuration.
     *
     * @param formatted The human-readable string to parse
     * @return ParsedPattern containing pattern, days, and interval
     */
    public static ParsedPattern parseFromString(String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return new ParsedPattern(RepeatPattern.DAILY, "[]", 1);
        }

        formatted = formatted.trim();

        // Check for Daily
        if (formatted.equals(PREFIX_DAILY)) {
            return new ParsedPattern(RepeatPattern.DAILY, "[]", 1);
        }

        // Check for Weekly pattern
        if (formatted.startsWith(PREFIX_WEEKLY)) {
            String daysStr = formatted.substring(PREFIX_WEEKLY.length());
            List<Integer> days = parseDayNames(daysStr);
            return new ParsedPattern(RepeatPattern.WEEKLY, daysToJson(days), 1);
        }

        // Check for Custom pattern (Every N days)
        if (formatted.startsWith(PREFIX_EVERY) && formatted.endsWith(SUFFIX_DAYS)) {
            try {
                String numberStr = formatted.substring(PREFIX_EVERY.length(), 
                    formatted.length() - SUFFIX_DAYS.length()).trim();
                int interval = Integer.parseInt(numberStr);
                return new ParsedPattern(RepeatPattern.CUSTOM, "[]", interval);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }

        // Default to DAILY if parsing fails
        return new ParsedPattern(RepeatPattern.DAILY, "[]", 1);
    }

    /**
     * Format weekly pattern with day names.
     */
    private static String formatWeeklyPattern(String repeatDaysJson) {
        List<Integer> days = parseDaysJson(repeatDaysJson);
        
        if (days.isEmpty()) {
            return PREFIX_WEEKLY + "No days selected";
        }

        StringBuilder sb = new StringBuilder(PREFIX_WEEKLY);
        for (int i = 0; i < days.size(); i++) {
            int dayIndex = days.get(i);
            if (dayIndex >= 0 && dayIndex < DAY_ABBREVIATIONS.length) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(DAY_ABBREVIATIONS[dayIndex]);
            }
        }

        return sb.toString();
    }

    /**
     * Format custom interval pattern.
     */
    private static String formatCustomPattern(int intervalDays) {
        if (intervalDays <= 0) {
            intervalDays = 1;
        }
        return PREFIX_EVERY + intervalDays + SUFFIX_DAYS;
    }

    /**
     * Parse JSON array string to list of day indices.
     */
    private static List<Integer> parseDaysJson(String json) {
        List<Integer> days = new ArrayList<>();
        
        if (json == null || json.isEmpty() || json.equals("[]")) {
            return days;
        }

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                days.add(array.getInt(i));
            }
        } catch (JSONException e) {
            // Return empty list on parse error
        }

        return days;
    }

    /**
     * Parse comma-separated day abbreviations to list of day indices.
     */
    private static List<Integer> parseDayNames(String daysStr) {
        List<Integer> days = new ArrayList<>();
        
        if (daysStr == null || daysStr.isEmpty()) {
            return days;
        }

        String[] parts = daysStr.split(",");
        for (String part : parts) {
            String dayName = part.trim();
            int index = findDayIndex(dayName);
            if (index >= 0) {
                days.add(index);
            }
        }

        return days;
    }

    /**
     * Find day index from name or abbreviation.
     */
    private static int findDayIndex(String dayName) {
        for (int i = 0; i < DAY_NAMES.length; i++) {
            if (DAY_NAMES[i].equalsIgnoreCase(dayName) || 
                DAY_ABBREVIATIONS[i].equalsIgnoreCase(dayName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Convert list of day indices to JSON array string.
     */
    private static String daysToJson(List<Integer> days) {
        JSONArray array = new JSONArray();
        for (Integer day : days) {
            array.put(day);
        }
        return array.toString();
    }

    /**
     * Result class for parsed pattern information.
     */
    public static class ParsedPattern {
        public final RepeatPattern pattern;
        public final String repeatDaysJson;
        public final int customIntervalDays;

        public ParsedPattern(RepeatPattern pattern, String repeatDaysJson, int customIntervalDays) {
            this.pattern = pattern;
            this.repeatDaysJson = repeatDaysJson;
            this.customIntervalDays = customIntervalDays;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ParsedPattern that = (ParsedPattern) o;
            return customIntervalDays == that.customIntervalDays &&
                   pattern == that.pattern &&
                   objectsEquals(repeatDaysJson, that.repeatDaysJson);
        }

        private static boolean objectsEquals(Object a, Object b) {
            return (a == b) || (a != null && a.equals(b));
        }

        @Override
        public int hashCode() {
            int result = pattern != null ? pattern.hashCode() : 0;
            result = 31 * result + (repeatDaysJson != null ? repeatDaysJson.hashCode() : 0);
            result = 31 * result + customIntervalDays;
            return result;
        }
    }
}
