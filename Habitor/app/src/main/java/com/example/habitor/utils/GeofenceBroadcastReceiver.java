package com.example.habitor.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import com.example.habitor.model.LocationTriggerType;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * Receives geofence transition events and triggers notifications.
 * Requirements: 4.3, 4.4
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null");
            return;
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: " + geofencingEvent.getErrorCode());
            return;
        }

        int transitionType = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.w(TAG, "No triggering geofences");
            return;
        }

        for (Geofence geofence : triggeringGeofences) {
            String requestId = geofence.getRequestId();
            int habitId = GeofenceManager.getHabitIdFromRequestId(requestId);
            
            if (habitId != -1) {
                handleGeofenceTransition(context, habitId, transitionType);
            }
        }
    }

    private void handleGeofenceTransition(Context context, int habitId, int transitionType) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                Habit habit = db.habitDao().getHabitById(habitId);

                if (habit == null || !habit.isLocationReminderEnabled()) {
                    Log.d(TAG, "Habit not found or location reminder disabled: " + habitId);
                    return;
                }

                LocationTriggerType triggerType = habit.getLocationTriggerTypeEnum();
                boolean shouldNotify = shouldTriggerNotification(transitionType, triggerType);

                if (shouldNotify) {
                    Log.d(TAG, "Triggering notification for habit: " + habit.getName());
                    sendNotification(context, habit, transitionType);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling geofence transition", e);
            }
        });
    }

    /**
     * Determine if notification should be triggered based on transition and trigger type.
     * Property 3: Geofence Trigger Correctness
     */
    public static boolean shouldTriggerNotification(int transitionType, LocationTriggerType triggerType) {
        if (triggerType == LocationTriggerType.ENTER && 
                transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            return true;
        }
        if (triggerType == LocationTriggerType.EXIT && 
                transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
            return true;
        }
        return false;
    }

    private void sendNotification(Context context, Habit habit, int transitionType) {
        String title;
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            title = "📍 You've arrived! Time for: " + habit.getName();
        } else {
            title = "📍 Leaving? Don't forget: " + habit.getName();
        }

        // Use static method from NotificationHelper
        NotificationHelper.showNotification(context, title, 
                habit.getLocationName() != null ? habit.getLocationName() : "Location reminder");
    }
}
