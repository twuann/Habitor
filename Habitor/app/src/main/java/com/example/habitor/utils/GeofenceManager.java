package com.example.habitor.utils;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.habitor.model.Habit;
import com.example.habitor.model.LocationTriggerType;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages geofence registration and removal for location-based habit reminders.
 * Requirements: 4.3, 4.4, 7.1, 7.2, 7.3, 7.4
 */
public class GeofenceManager {

    private static final String TAG = "GeofenceManager";
    private static final int MAX_GEOFENCES = 100;
    private static final long GEOFENCE_EXPIRATION = Geofence.NEVER_EXPIRE;

    private final Context context;
    private final GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;

    public interface GeofenceCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public GeofenceManager(Context context) {
        this.context = context.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(context);
    }

    /**
     * Register a geofence for a habit with location reminder enabled.
     */
    public void registerGeofence(Habit habit, GeofenceCallback callback) {
        if (!habit.hasLocation() || !habit.isLocationReminderEnabled()) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Habit has no location or reminder disabled"));
            }
            return;
        }

        if (!hasLocationPermission()) {
            if (callback != null) {
                callback.onFailure(new SecurityException("Location permission not granted"));
            }
            return;
        }

        Geofence geofence = buildGeofence(habit);
        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        try {
            geofencingClient.addGeofences(request, getGeofencePendingIntent())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Geofence registered for habit: " + habit.getId());
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to register geofence: " + e.getMessage());
                        if (callback != null) callback.onFailure(e);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            if (callback != null) callback.onFailure(e);
        }
    }

    /**
     * Unregister geofence for a specific habit.
     */
    public void unregisterGeofence(int habitId, GeofenceCallback callback) {
        String requestId = getGeofenceRequestId(habitId);
        
        geofencingClient.removeGeofences(Collections.singletonList(requestId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofence removed for habit: " + habitId);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove geofence: " + e.getMessage());
                    if (callback != null) callback.onFailure(e);
                });
    }

    /**
     * Re-register all geofences (called after device boot).
     */
    public void reregisterAllGeofences(List<Habit> habits, GeofenceCallback callback) {
        if (habits == null || habits.isEmpty()) {
            if (callback != null) callback.onSuccess();
            return;
        }

        List<Geofence> geofences = new ArrayList<>();
        int count = 0;

        for (Habit habit : habits) {
            if (habit.hasLocation() && habit.isLocationReminderEnabled() && count < MAX_GEOFENCES) {
                geofences.add(buildGeofence(habit));
                count++;
            }
        }

        if (geofences.isEmpty()) {
            if (callback != null) callback.onSuccess();
            return;
        }

        if (!hasLocationPermission()) {
            if (callback != null) {
                callback.onFailure(new SecurityException("Location permission not granted"));
            }
            return;
        }

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build();

        try {
            geofencingClient.addGeofences(request, getGeofencePendingIntent())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Re-registered " + geofences.size() + " geofences");
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to re-register geofences: " + e.getMessage());
                        if (callback != null) callback.onFailure(e);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            if (callback != null) callback.onFailure(e);
        }
    }

    /**
     * Build a Geofence object from a Habit.
     */
    private Geofence buildGeofence(Habit habit) {
        int transitionType;
        LocationTriggerType triggerType = habit.getLocationTriggerTypeEnum();
        if (triggerType == LocationTriggerType.EXIT) {
            transitionType = Geofence.GEOFENCE_TRANSITION_EXIT;
        } else {
            transitionType = Geofence.GEOFENCE_TRANSITION_ENTER;
        }

        return new Geofence.Builder()
                .setRequestId(getGeofenceRequestId(habit.getId()))
                .setCircularRegion(
                        habit.getLatitude(),
                        habit.getLongitude(),
                        habit.getLocationRadius())
                .setExpirationDuration(GEOFENCE_EXPIRATION)
                .setTransitionTypes(transitionType)
                .build();
    }

    /**
     * Get unique request ID for a habit's geofence.
     */
    public static String getGeofenceRequestId(int habitId) {
        return "habit_geofence_" + habitId;
    }

    /**
     * Extract habit ID from geofence request ID.
     */
    public static int getHabitIdFromRequestId(String requestId) {
        if (requestId != null && requestId.startsWith("habit_geofence_")) {
            try {
                return Integer.parseInt(requestId.substring("habit_geofence_".length()));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        return geofencePendingIntent;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Validate and clamp radius to valid range (50-500 meters).
     */
    public static int validateRadius(int radius) {
        return Math.max(50, Math.min(500, radius));
    }
}
