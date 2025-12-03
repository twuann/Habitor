package com.example.habitor.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * BroadcastReceiver that listens for device boot completion.
 * Reschedules all active habit reminders after device restart.
 * 
 * Requirements: 4.5 - WHEN the device restarts THEN the Habitor System 
 * SHALL reschedule all active reminders automatically
 * Requirements: 7.2 - WHEN the device restarts THEN the Habitor System 
 * SHALL re-register all active geofences automatically
 */
public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "Received null intent or action");
            return;
        }
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device boot completed - rescheduling habit reminders");
            
            // Create AlarmScheduler and reschedule all reminders
            AlarmScheduler alarmScheduler = new AlarmScheduler(context);
            alarmScheduler.rescheduleAllReminders();
            
            // Also reschedule the end-of-day reminder for high priority habits
            alarmScheduler.scheduleEndOfDayReminder();
            
            // Re-register all geofences for location-based reminders
            reregisterGeofences(context);
            
            Log.d(TAG, "Initiated rescheduling of all habit reminders and end-of-day reminder");
        }
    }
    
    private void reregisterGeofences(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<Habit> habits = db.habitDao().getAll();
                
                GeofenceManager geofenceManager = new GeofenceManager(context);
                geofenceManager.reregisterAllGeofences(habits, new GeofenceManager.GeofenceCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Successfully re-registered geofences after boot");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to re-register geofences: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error re-registering geofences", e);
            }
        });
    }
}
