package com.example.habitor.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import com.example.habitor.model.HabitDao;
import com.example.habitor.model.HabitHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BroadcastReceiver that handles end-of-day reminders for incomplete high priority habits.
 * Triggered at 8 PM daily to check for incomplete high priority habits.
 * 
 * Requirements: 9.5 - WHEN a High priority habit is incomplete near end of day 
 * THEN the Habitor System SHALL send an additional reminder notification
 */
public class EndOfDayReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "EndOfDayReminderReceiver";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "End of day reminder check triggered");
        
        executor.execute(() -> {
            try {
                checkAndNotifyIncompleteHighPriorityHabits(context);
                
                // Reschedule for next day
                AlarmScheduler scheduler = new AlarmScheduler(context);
                scheduler.scheduleEndOfDayReminder();
            } catch (Exception e) {
                Log.e(TAG, "Error checking incomplete high priority habits", e);
            }
        });
    }

    /**
     * Check for incomplete high priority habits and send notifications.
     */
    private void checkAndNotifyIncompleteHighPriorityHabits(Context context) {
        HabitDao habitDao = AppDatabase.getInstance(context).habitDao();
        
        // Get all high priority habits
        List<Habit> highPriorityHabits = habitDao.getHighPriorityHabits();
        
        if (highPriorityHabits == null || highPriorityHabits.isEmpty()) {
            Log.d(TAG, "No high priority habits found");
            return;
        }
        
        // Get today's date in yyyy-MM-dd format
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // Get completed habits for today
        List<HabitHistory> todayHistory = habitDao.getHistoryForDate(today);
        Set<String> completedHabitNames = new HashSet<>();
        if (todayHistory != null) {
            for (HabitHistory history : todayHistory) {
                completedHabitNames.add(history.habitName);
            }
        }
        
        // Find incomplete high priority habits
        List<Habit> incompleteHabits = new ArrayList<>();
        for (Habit habit : highPriorityHabits) {
            if (!completedHabitNames.contains(habit.getName())) {
                incompleteHabits.add(habit);
            }
        }
        
        Log.d(TAG, "Found " + incompleteHabits.size() + " incomplete high priority habits");
        
        // Send notifications for incomplete habits
        for (Habit habit : incompleteHabits) {
            NotificationHelper.showHighPriorityReminder(context, habit);
            Log.d(TAG, "Sent end-of-day reminder for: " + habit.getName());
        }
    }
}
