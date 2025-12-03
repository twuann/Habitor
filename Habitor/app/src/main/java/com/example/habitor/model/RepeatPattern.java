package com.example.habitor.model;

/**
 * Repeat patterns for habit reminders.
 * Defines how often a reminder should trigger.
 */
public enum RepeatPattern {
    DAILY,      // Every day
    WEEKLY,     // Specific days of week
    CUSTOM;     // Every N days

    /**
     * Parse a string to RepeatPattern enum, defaulting to DAILY if invalid.
     */
    public static RepeatPattern fromString(String value) {
        if (value == null) {
            return DAILY;
        }
        try {
            return RepeatPattern.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DAILY;
        }
    }
}
