package com.example.habitor.model;

/**
 * Priority levels for habits.
 * Used to sort and filter habits by importance.
 */
public enum Priority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int sortOrder;

    Priority(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * Parse a string to Priority enum, defaulting to MEDIUM if invalid.
     */
    public static Priority fromString(String value) {
        if (value == null) {
            return MEDIUM;
        }
        try {
            return Priority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
