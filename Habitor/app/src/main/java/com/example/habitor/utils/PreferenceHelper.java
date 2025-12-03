package com.example.habitor.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class for managing user preferences and settings.
 * Handles user profile info, device ID, and sync preferences.
 * 
 * Requirements: 2.2, 2.5
 */
public class PreferenceHelper {

    private static final String PREF_NAME = "habitor_prefs";

    private static final String KEY_NAME = "user_name";
    private static final String KEY_AGE = "user_age";
    private static final String KEY_GENDER = "user_gender";
    private static final String KEY_ONBOARD_DONE = "onboard_done";

    private static final String KEY_IMAGE = "user_image";
    
    // Device User ID keys (Requirements: 2.2)
    private static final String KEY_DEVICE_USER_ID = "device_user_id";
    
    // Sync preferences keys (Requirements: 2.5)
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    private static final String KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled";
    private static final String KEY_SYNC_ON_WIFI_ONLY = "sync_on_wifi_only";
    private static final String KEY_FIRESTORE_INITIALIZED = "firestore_initialized";
    
    // Firebase Auth state keys (Requirements: 2.5, 5.3)
    private static final String KEY_FIREBASE_UID = "firebase_uid";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_IS_SIGNED_IN = "is_signed_in";

    // ==========================
    // SAVE USER INFO
    // ==========================
    public static void saveUserInfo(Context context, String name, int age, String gender) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_NAME, name)
                .putInt(KEY_AGE, age)
                .putString(KEY_GENDER, gender)
                .putBoolean(KEY_ONBOARD_DONE, true)
                .apply();
    }

    // ==========================
    // GET INFO
    // ==========================
    public static String getUserName(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_NAME, "Your Name");
    }

    public static int getUserAge(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_AGE, 0);
    }

    public static String getUserGender(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_GENDER, "");
    }

    // ==========================
    // IMAGE SAVE / GET
    // ==========================
    public static void saveUserImage(Context context, String imageUri) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_IMAGE, imageUri).apply();
    }

    public static String getUserImage(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_IMAGE, "");
    }

    // ==========================
    public static boolean isOnboarded(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ONBOARD_DONE, false);
    }

    // ==========================
    // DEVICE USER ID (Requirements: 2.2)
    // ==========================
    
    /**
     * Save the device user ID to SharedPreferences.
     * This ID is used to identify the user across Firestore sync operations.
     *
     * @param context Application context
     * @param deviceUserId The unique device-based user ID
     */
    public static void saveDeviceUserId(Context context, String deviceUserId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_DEVICE_USER_ID, deviceUserId)
                .apply();
    }

    /**
     * Get the device user ID from SharedPreferences.
     *
     * @param context Application context
     * @return The device user ID, or null if not set
     */
    public static String getDeviceUserId(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DEVICE_USER_ID, null);
    }

    /**
     * Check if a device user ID has been saved.
     *
     * @param context Application context
     * @return true if device user ID exists, false otherwise
     */
    public static boolean hasDeviceUserId(Context context) {
        String deviceId = getDeviceUserId(context);
        return deviceId != null && !deviceId.isEmpty();
    }

    // ==========================
    // SYNC PREFERENCES (Requirements: 2.5)
    // ==========================

    /**
     * Save the last sync timestamp.
     *
     * @param context Application context
     * @param timestamp The timestamp of the last successful sync
     */
    public static void saveLastSyncTime(Context context, long timestamp) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_LAST_SYNC_TIME, timestamp)
                .apply();
    }

    /**
     * Get the last sync timestamp.
     *
     * @param context Application context
     * @return The timestamp of the last sync, or 0 if never synced
     */
    public static long getLastSyncTime(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SYNC_TIME, 0);
    }

    /**
     * Set whether auto-sync is enabled.
     *
     * @param context Application context
     * @param enabled true to enable auto-sync, false to disable
     */
    public static void setAutoSyncEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_AUTO_SYNC_ENABLED, enabled)
                .apply();
    }

    /**
     * Check if auto-sync is enabled.
     *
     * @param context Application context
     * @return true if auto-sync is enabled (default: true)
     */
    public static boolean isAutoSyncEnabled(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_SYNC_ENABLED, true);
    }

    /**
     * Set whether sync should only occur on WiFi.
     *
     * @param context Application context
     * @param wifiOnly true to sync only on WiFi, false to sync on any connection
     */
    public static void setSyncOnWifiOnly(Context context, boolean wifiOnly) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_SYNC_ON_WIFI_ONLY, wifiOnly)
                .apply();
    }

    /**
     * Check if sync should only occur on WiFi.
     *
     * @param context Application context
     * @return true if sync is WiFi-only (default: false)
     */
    public static boolean isSyncOnWifiOnly(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SYNC_ON_WIFI_ONLY, false);
    }

    /**
     * Set whether Firestore user document has been initialized.
     *
     * @param context Application context
     * @param initialized true if Firestore has been initialized
     */
    public static void setFirestoreInitialized(Context context, boolean initialized) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_FIRESTORE_INITIALIZED, initialized)
                .apply();
    }

    /**
     * Check if Firestore user document has been initialized.
     *
     * @param context Application context
     * @return true if Firestore has been initialized
     */
    public static boolean isFirestoreInitialized(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_FIRESTORE_INITIALIZED, false);
    }

    /**
     * Clear all sync-related preferences.
     * Useful for logout or reset scenarios.
     *
     * @param context Application context
     */
    public static void clearSyncPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_LAST_SYNC_TIME)
                .remove(KEY_FIRESTORE_INITIALIZED)
                .apply();
    }

    // ==========================
    // FIREBASE AUTH STATE (Requirements: 2.5, 5.3)
    // ==========================

    /**
     * Save the Firebase authentication state.
     * Called after successful sign in to persist auth state across app restarts.
     *
     * @param context Application context
     * @param uid The Firebase user ID
     * @param email The user's email address
     */
    public static void saveAuthState(Context context, String uid, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_FIREBASE_UID, uid)
                .putString(KEY_USER_EMAIL, email)
                .putBoolean(KEY_IS_SIGNED_IN, true)
                .apply();
    }

    /**
     * Clear the Firebase authentication state.
     * Called on sign out to remove persisted auth state.
     *
     * @param context Application context
     */
    public static void clearAuthState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_FIREBASE_UID)
                .remove(KEY_USER_EMAIL)
                .putBoolean(KEY_IS_SIGNED_IN, false)
                .apply();
    }

    /**
     * Get the Firebase user ID.
     *
     * @param context Application context
     * @return The Firebase UID, or null if not signed in
     */
    public static String getFirebaseUid(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FIREBASE_UID, null);
    }

    /**
     * Get the user's email address.
     *
     * @param context Application context
     * @return The user's email, or null if not signed in
     */
    public static String getUserEmail(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_EMAIL, null);
    }

    /**
     * Check if the user is signed in with Firebase.
     *
     * @param context Application context
     * @return true if signed in, false otherwise
     */
    public static boolean isSignedIn(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_SIGNED_IN, false);
    }
}
