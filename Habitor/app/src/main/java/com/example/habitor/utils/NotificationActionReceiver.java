package com.example.habitor.utils;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import com.example.habitor.model.HabitDao;
import com.example.habitor.model.HabitHistory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BroadcastReceiver that handles notification action button clicks.
 * 
 * Requirements:
 * - 6.2: Record habit completion when "Mark Complete" is tapped
 * - 6.3: Reschedule reminder for 10 minutes when "Snooze" is tapped
 */
public class NotificationActionReceiver extends BroadcastReceiver {
    
    private static final String TAG = "NotificationActionReceiver";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "Received null intent or action");
            return;
        }

        int habitId = intent.getIntExtra(NotificationHelper.EXTRA_HABIT_ID, -1);
        String habitName = intent.getStringExtra(NotificationHelper.EXTRA_HABIT_NAME);

        if (habitId == -1) {
            Log.e(TAG, "Invalid habit ID received");
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action + " for habit: " + habitName);

        switch (action) {
            case NotificationHelper.ACTION_MARK_COMPLETE:
                handleMarkComplete(context, habitId, habitName);
                break;
            case NotificationHelper.ACTION_SNOOZE:
                handleSnooze(context, habitId, habitName);
                break;
            default:
                Log.w(TAG, "Unknown action: " + action);
        }
    }


    /**
     * Handle the "Mark Complete" action from notification.
     * Requirements: 6.2 - Record habit completion for the current date
     * 
     * @param context The application context
     * @param habitId The ID of the habit to mark complete
     * @param habitName The name of the habit
     */
    private void handleMarkComplete(Context context, int habitId, String habitName) {
        // Dismiss the notification
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(habitId);

        // Record completion in database
        executor.execute(() -> {
            try {
                HabitDao habitDao = AppDatabase.getInstance(context).habitDao();
                Habit habit = habitDao.getHabitById(habitId);
                
                if (habit != null) {
                    // Create history entry for today
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(new Date());
                    
                    HabitHistory history = new HabitHistory();
                    history.habitName = habit.getName();
                    history.date = today;
                    
                    habitDao.insertHistory(history);
                    
                    // Update streak count
                    int newStreak = habit.getStreakCount() + 1;
                    habit.setStreakCount(newStreak);
                    habitDao.update(habit);
                    
                    Log.d(TAG, "Marked habit complete: " + habitName + ", new streak: " + newStreak);
                    
                    // Show confirmation toast on main thread
                    android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                    mainHandler.post(() -> {
                        Toast.makeText(context, 
                                "✓ " + habitName + " completed! 🎉", 
                                Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error marking habit complete", e);
            }
        });
    }

    /**
     * Handle the "Snooze" action from notification.
     * Requirements: 6.3 - Reschedule reminder for 10 minutes
     * 
     * @param context The application context
     * @param habitId The ID of the habit to snooze
     * @param habitName The name of the habit
     */
    private void handleSnooze(Context context, int habitId, String habitName) {
        // Dismiss the notification
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(habitId);

        // Snooze the reminder for 10 minutes
        AlarmScheduler alarmScheduler = new AlarmScheduler(context);
        alarmScheduler.snoozeReminder(habitId);
        
        Log.d(TAG, "Snoozed reminder for habit: " + habitName);
        
        // Show confirmation toast
        Toast.makeText(context, 
                "⏰ Snoozed for 10 minutes", 
                Toast.LENGTH_SHORT).show();
    }
}
