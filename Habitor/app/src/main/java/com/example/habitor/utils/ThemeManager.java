package com.example.habitor.utils;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Manages theme switching and persistence for the Habitor app.
 * Supports Light, Dark, and System Default themes.
 * 
 * Requirements: 1.2, 1.3, 1.4
 */
public class ThemeManager {

    /**
     * Enum representing the available theme modes.
     */
    public enum ThemeMode {
        LIGHT,   // Force light theme
        DARK,    // Force dark theme
        SYSTEM   // Follow system setting
    }

    /**
     * Apply the specified theme mode immediately.
     * This will update the app's appearance without requiring a restart.
     *
     * @param mode The ThemeMode to apply (LIGHT, DARK, or SYSTEM)
     * 
     * Requirements: 1.2, 1.3, 1.4
     */
    public static void applyTheme(ThemeMode mode) {
        int nightMode = toNightMode(mode);
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    /**
     * Convert ThemeMode to AppCompatDelegate night mode constant.
     * 
     * Mapping:
     * - LIGHT  → MODE_NIGHT_NO
     * - DARK   → MODE_NIGHT_YES
     * - SYSTEM → MODE_NIGHT_FOLLOW_SYSTEM
     *
     * @param mode The ThemeMode to convert
     * @return The corresponding AppCompatDelegate night mode constant
     * 
     * Requirements: 1.2, 1.3, 1.4
     */
    public static int toNightMode(ThemeMode mode) {
        if (mode == null) {
            return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        
        switch (mode) {
            case LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case SYSTEM:
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    /**
     * Save the theme preference to SharedPreferences.
     *
     * @param context Application context
     * @param mode The ThemeMode to save
     * 
     * Requirements: 1.5, 1.6
     */
    public static void saveThemePreference(Context context, ThemeMode mode) {
        String modeString = mode != null ? mode.name() : ThemeMode.SYSTEM.name();
        PreferenceHelper.saveThemeMode(context, modeString);
    }

    /**
     * Load the saved theme preference from SharedPreferences.
     *
     * @param context Application context
     * @return The saved ThemeMode, or SYSTEM if not set or invalid
     * 
     * Requirements: 1.5, 1.6
     */
    public static ThemeMode getThemePreference(Context context) {
        String modeString = PreferenceHelper.getThemeMode(context);
        return fromString(modeString);
    }

    /**
     * Convert a string to ThemeMode enum.
     * Returns SYSTEM as default for null or invalid strings.
     *
     * @param modeString The string representation of ThemeMode
     * @return The corresponding ThemeMode, or SYSTEM if invalid
     */
    public static ThemeMode fromString(String modeString) {
        if (modeString == null || modeString.isEmpty()) {
            return ThemeMode.SYSTEM;
        }
        
        try {
            return ThemeMode.valueOf(modeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ThemeMode.SYSTEM;
        }
    }
}
