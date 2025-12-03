package com.example.habitor.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver that handles alarm triggers for habit reminders.
 * 
 * Requirements:
 * - 4.3: Display push notification with habit name and motivational message when alarm triggers
 */
public class AlarmReceiver extends BroadcastReceiver {
    
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received");
        
        // Extract habit info from intent
        int habitId = intent.getIntExtra(AlarmScheduler.EXTRA_HABIT_ID, -1);
        String habitName = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_NAME);
        String habitCategory = intent.getStringExtra(AlarmScheduler.EXTRA_HABIT_CATEGORY);
        
        if (habitId != -1 && habitName != null) {
            // Show enhanced notification with action buttons
            NotificationHelper.showHabitReminder(context, habitId, habitName, habitCategory);
            Log.d(TAG, "Showed reminder notification for habit: " + habitName);
            
            // Reschedule the alarm for the next occurrence
            rescheduleNextAlarm(context, habitId);
        } else {
            // Fallback to generic notification if habit info is missing
            NotificationHelper.showNotification(context,
                    "Habitor Reminder 🌿",
                    "Don't forget your daily habits today!");
            Log.w(TAG, "Habit info missing from intent, showing generic notification");
        }
    }

    /**
     * Reschedule the alarm for the next occurrence based on repeat pattern.
     */
    private void rescheduleNextAlarm(Context context, int habitId) {
        // This will be handled asynchronously by AlarmScheduler
        // The scheduler will query the habit from database and calculate next trigger time
        new Thread(() -> {
            try {
                com.example.habitor.model.HabitDao habitDao = 
                        com.example.habitor.model.AppDatabase.getInstance(context).habitDao();
                com.example.habitor.model.Habit habit = habitDao.getHabitById(habitId);
                
                if (habit != null && habit.isReminderEnabled()) {
                    AlarmScheduler scheduler = new AlarmScheduler(context);
                    scheduler.scheduleReminder(habit);
                    Log.d(TAG, "Rescheduled next alarm for habit: " + habit.getName());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error rescheduling alarm", e);
            }
        }).start();
    }
}
