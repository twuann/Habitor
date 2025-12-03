package com.example.habitor.utils;

import com.example.habitor.model.HabitHistory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Utility class for calculating habit streaks and statistics.
 * Provides methods to calculate current streak, longest streak, and completion rate.
 */
public class StreakCalculator {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);

    /**
     * Calculate the current streak count for a habit.
     * A streak is the number of consecutive days of completion ending with today or yesterday.
     *
     * @param history List of HabitHistory entries for the habit
     * @return Current streak count (0 if no streak)
     */
    public static int calculateCurrentStreak(List<HabitHistory> history) {
        if (history == null || history.isEmpty()) {
            return 0;
        }

        Set<String> completedDates = extractCompletedDates(history);
        if (completedDates.isEmpty()) {
            return 0;
        }

        // Get today and yesterday dates
        Calendar calendar = Calendar.getInstance();
        String today = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = sdf.format(calendar.getTime());

        // Check if streak is active (completed today or yesterday)
        if (!completedDates.contains(today) && !completedDates.contains(yesterday)) {
            return 0;
        }

        // Start counting from today or yesterday
        String startDate = completedDates.contains(today) ? today : yesterday;
        
        return countConsecutiveDaysBackward(completedDates, startDate);
    }

    /**
     * Calculate the longest streak ever achieved for a habit.
     *
     * @param history List of HabitHistory entries for the habit
     * @return Longest streak count (0 if no history)
     */
    public static int calculateLongestStreak(List<HabitHistory> history) {
        if (history == null || history.isEmpty()) {
            return 0;
        }

        Set<String> completedDates = extractCompletedDates(history);
        if (completedDates.isEmpty()) {
            return 0;
        }

        // Sort dates to find consecutive sequences
        List<String> sortedDates = new ArrayList<>(completedDates);
        Collections.sort(sortedDates);

        int longestStreak = 0;
        int currentStreak = 1;

        for (int i = 1; i < sortedDates.size(); i++) {
            if (areDatesConsecutive(sortedDates.get(i - 1), sortedDates.get(i))) {
                currentStreak++;
            } else {
                longestStreak = Math.max(longestStreak, currentStreak);
                currentStreak = 1;
            }
        }

        return Math.max(longestStreak, currentStreak);
    }

    /**
     * Calculate the completion rate as a percentage.
     *
     * @param history List of HabitHistory entries for the habit
     * @param totalDays Total number of days to consider (e.g., days since habit creation)
     * @return Completion rate as a float between 0.0 and 100.0
     */
    public static float calculateCompletionRate(List<HabitHistory> history, int totalDays) {
        if (totalDays <= 0) {
            return 0.0f;
        }

        if (history == null || history.isEmpty()) {
            return 0.0f;
        }

        Set<String> completedDates = extractCompletedDates(history);
        int completedDays = completedDates.size();

        // Cap completed days at total days to avoid > 100%
        completedDays = Math.min(completedDays, totalDays);

        return (completedDays * 100.0f) / totalDays;
    }

    /**
     * Extract unique completed dates from history entries.
     */
    private static Set<String> extractCompletedDates(List<HabitHistory> history) {
        Set<String> dates = new HashSet<>();
        for (HabitHistory entry : history) {
            if (entry.date != null && !entry.date.isEmpty()) {
                dates.add(entry.date);
            }
        }
        return dates;
    }

    /**
     * Count consecutive days backward from a starting date.
     */
    private static int countConsecutiveDaysBackward(Set<String> completedDates, String startDate) {
        int streak = 0;
        Calendar calendar = Calendar.getInstance();
        
        try {
            Date date = sdf.parse(startDate);
            if (date != null) {
                calendar.setTime(date);
            }
        } catch (ParseException e) {
            return 0;
        }

        while (completedDates.contains(sdf.format(calendar.getTime()))) {
            streak++;
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }

        return streak;
    }

    /**
     * Check if two date strings represent consecutive days.
     */
    private static boolean areDatesConsecutive(String date1, String date2) {
        try {
            Date d1 = sdf.parse(date1);
            Date d2 = sdf.parse(date2);
            
            if (d1 == null || d2 == null) {
                return false;
            }

            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(d1);
            cal1.add(Calendar.DAY_OF_YEAR, 1);

            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(d2);

            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        } catch (ParseException e) {
            return false;
        }
    }
}
