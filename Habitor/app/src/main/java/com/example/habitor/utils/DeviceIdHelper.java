package com.example.habitor.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Utility class for generating and managing unique device-based user IDs.
 * The device ID is generated once and persisted in SharedPreferences.
 */
public class DeviceIdHelper {

    private static final String PREF_NAME = "habitor_device_prefs";
    private static final String KEY_DEVICE_USER_ID = "device_user_id";

    /**
     * Get the device user ID. If one doesn't exist, generate and save a new one.
     *
     * @param context Application context
     * @return Unique device user ID
     */
    public static String getDeviceUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String deviceId = prefs.getString(KEY_DEVICE_USER_ID, null);

        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = generateDeviceUserId();
            saveDeviceUserId(context, deviceId);
        }

        return deviceId;
    }

    /**
     * Save a device user ID to SharedPreferences.
     *
     * @param context Application context
     * @param deviceId The device user ID to save
     */
    public static void saveDeviceUserId(Context context, String deviceId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_DEVICE_USER_ID, deviceId)
            .apply();
    }

    /**
     * Check if a device user ID has been generated.
     *
     * @param context Application context
     * @return true if device ID exists, false otherwise
     */
    public static boolean hasDeviceUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String deviceId = prefs.getString(KEY_DEVICE_USER_ID, null);
        return deviceId != null && !deviceId.isEmpty();
    }

    /**
     * Clear the stored device user ID.
     * Use with caution - this will break cloud sync association.
     *
     * @param context Application context
     */
    public static void clearDeviceUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .remove(KEY_DEVICE_USER_ID)
            .apply();
    }

    /**
     * Generate a new unique device user ID.
     * Uses UUID for guaranteed uniqueness.
     *
     * @return New unique device user ID
     */
    private static String generateDeviceUserId() {
        return "device_" + UUID.randomUUID().toString();
    }

    /**
     * Generate a device user ID without saving it.
     * Useful for testing or when you need to generate an ID before deciding to save.
     *
     * @return New unique device user ID (not persisted)
     */
    public static String generateNewDeviceUserId() {
        return generateDeviceUserId();
    }
}
