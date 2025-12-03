package com.example.habitor.model;

/**
 * Enum representing the trigger type for location-based reminders.
 * Requirements: 4.1
 */
public enum LocationTriggerType {
    ENTER("When I arrive"),
    EXIT("When I leave");

    private final String displayName;

    LocationTriggerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convert a string to LocationTriggerType enum.
     * @param value String value to convert
     * @return Corresponding enum value, defaults to ENTER if invalid
     */
    public static LocationTriggerType fromString(String value) {
        if (value == null) {
            return ENTER;
        }
        try {
            return LocationTriggerType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ENTER;
        }
    }
}
