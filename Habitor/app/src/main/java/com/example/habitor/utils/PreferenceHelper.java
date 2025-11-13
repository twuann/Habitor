package com.example.habitor.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceHelper {

    private static final String PREF_NAME = "habitor_prefs";

    private static final String KEY_NAME = "user_name";
    private static final String KEY_AGE = "user_age";
    private static final String KEY_GENDER = "user_gender";
    private static final String KEY_ONBOARD_DONE = "onboard_done";

    private static final String KEY_IMAGE = "user_image"; // NEW

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
}
