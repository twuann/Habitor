package com.example.habitor.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import com.example.habitor.model.HabitDao;
import com.example.habitor.model.RepeatPattern;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AlarmScheduler manages habit reminder scheduling using AlarmManager.
 * 
 * Requirements:
 * - 4.2: Schedule local alarm using AlarmManager for specific time
 * - 4.4: Cancel scheduled alarm when reminder is disabled
 * - 4.5: Reschedule all active reminders after device restart
 * - 6.3: Snooze reminder for 10 minutes
 */
public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    public static final String EXTRA_HABIT_ID = "habit_id";
    public static final String EXTRA_HABIT_NAME = "habit_name";
    public static final String EXTRA_HABIT_CATEGORY = "habit_category";
    public static final int SNOOZE_DURATION_MINUTES = 10;
    
    // End of day reminder constants
    private static final int END_OF_DAY_REMINDER_HOUR = 20; // 8 PM
    private static final int END_OF_DAY_REMINDER_MINUTE = 0;
    private static final int END_OF_DAY_REMINDER_REQUEST_CODE = 999999;

    private final Context context;
    private final AlarmManager alarmManager;
    private final ExecutorService executor;

    public AlarmScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.executor = Executors.newSingleThreadExecutor();
    }


    /**
     * Schedule a reminder for a habit.
     * Requirements: 4.2 - Schedule local alarm using AlarmManager for specific time
     * 
     * @param habit The habit to schedule reminder for
     */
    public void scheduleReminder(Habit habit) {
        if (habit == null || !habit.isReminderEnabled() || habit.getReminderTime() == null) {
            Log.d(TAG, "Skipping schedule - reminder not enabled or time not set");
            return;
        }

        long triggerTime = calculateNextTriggerTime(habit);
        if (triggerTime <= System.currentTimeMillis()) {
            // If calculated time is in the past, schedule for next occurrence
            triggerTime = calculateNextOccurrence(habit, triggerTime);
        }

        PendingIntent pendingIntent = createPendingIntent(habit);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                } else {
                    // Fallback to inexact alarm if exact alarm permission not granted
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }
            Log.d(TAG, "Scheduled reminder for habit: " + habit.getName() + " at " + triggerTime);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to schedule exact alarm - permission denied", e);
            // Fallback to inexact alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    /**
     * Cancel a scheduled reminder for a habit.
     * Requirements: 4.4 - Cancel scheduled alarm when reminder is disabled
     * 
     * @param habitId The ID of the habit to cancel reminder for
     */
    public void cancelReminder(int habitId) {
        PendingIntent pendingIntent = createPendingIntentForId(habitId);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.d(TAG, "Cancelled reminder for habit ID: " + habitId);
    }

    /**
     * Reschedule all active reminders.
     * Requirements: 4.5 - Reschedule all active reminders after device restart
     */
    public void rescheduleAllReminders() {
        executor.execute(() -> {
            try {
                HabitDao habitDao = AppDatabase.getInstance(context).habitDao();
                List<Habit> habits = habitDao.getAll();
                
                for (Habit habit : habits) {
                    if (habit.isReminderEnabled() && habit.getReminderTime() != null) {
                        scheduleReminder(habit);
                    }
                }
                Log.d(TAG, "Rescheduled " + habits.size() + " habit reminders");
            } catch (Exception e) {
                Log.e(TAG, "Error rescheduling reminders", e);
            }
        });
    }

    /**
     * Snooze a reminder for a specified duration.
     * Requirements: 6.3 - Snooze reminder for 10 minutes
     * 
     * @param habitId The ID of the habit to snooze
     */
    public void snoozeReminder(int habitId) {
        snoozeReminder(habitId, SNOOZE_DURATION_MINUTES);
    }

    /**
     * Snooze a reminder for a custom duration.
     * 
     * @param habitId The ID of the habit to snooze
     * @param minutes The number of minutes to snooze
     */
    public void snoozeReminder(int habitId, int minutes) {
        executor.execute(() -> {
            try {
                HabitDao habitDao = AppDatabase.getInstance(context).habitDao();
                Habit habit = habitDao.getHabitById(habitId);
                
                if (habit != null) {
                    long snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000L);
                    PendingIntent pendingIntent = createPendingIntent(habit);
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    snoozeTime,
                                    pendingIntent
                            );
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                snoozeTime,
                                pendingIntent
                        );
                    }
                    Log.d(TAG, "Snoozed reminder for habit: " + habit.getName() + " for " + minutes + " minutes");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error snoozing reminder", e);
            }
        });
    }


    /**
     * Calculate the next trigger time for a habit reminder.
     * 
     * @param habit The habit to calculate trigger time for
     * @return The next trigger time in milliseconds
     */
    public long calculateNextTriggerTime(Habit habit) {
        if (habit.getReminderTime() == null) {
            return 0;
        }

        String[] timeParts = habit.getReminderTime().split(":");
        if (timeParts.length != 2) {
            Log.e(TAG, "Invalid reminder time format: " + habit.getReminderTime());
            return 0;
        }

        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the time has already passed today, schedule for next occurrence
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return adjustForRepeatPattern(habit, calendar);
    }

    /**
     * Adjust the trigger time based on the repeat pattern.
     */
    private long adjustForRepeatPattern(Habit habit, Calendar calendar) {
        RepeatPattern pattern = habit.getRepeatPatternEnum();
        
        switch (pattern) {
            case DAILY:
                // Already set for next occurrence
                return calendar.getTimeInMillis();
                
            case WEEKLY:
                return adjustForWeeklyPattern(habit, calendar);
                
            case CUSTOM:
                // For custom interval, we just use the next day calculation
                // The actual interval is handled when rescheduling after trigger
                return calendar.getTimeInMillis();
                
            default:
                return calendar.getTimeInMillis();
        }
    }

    /**
     * Adjust trigger time for weekly repeat pattern.
     */
    private long adjustForWeeklyPattern(Habit habit, Calendar calendar) {
        String repeatDaysJson = habit.getRepeatDays();
        if (repeatDaysJson == null || repeatDaysJson.equals("[]")) {
            return calendar.getTimeInMillis();
        }

        try {
            JSONArray daysArray = new JSONArray(repeatDaysJson);
            if (daysArray.length() == 0) {
                return calendar.getTimeInMillis();
            }

            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            
            // Find the next scheduled day
            for (int i = 0; i < 7; i++) {
                int checkDay = ((currentDayOfWeek - 1 + i) % 7) + 1;
                for (int j = 0; j < daysArray.length(); j++) {
                    if (daysArray.getInt(j) == checkDay) {
                        if (i > 0) {
                            calendar.add(Calendar.DAY_OF_YEAR, i);
                        }
                        return calendar.getTimeInMillis();
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing repeat days", e);
        }

        return calendar.getTimeInMillis();
    }

    /**
     * Calculate the next occurrence after a given time.
     */
    private long calculateNextOccurrence(Habit habit, long currentTriggerTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTriggerTime);
        
        RepeatPattern pattern = habit.getRepeatPatternEnum();
        
        switch (pattern) {
            case DAILY:
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
                
            case WEEKLY:
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                return adjustForWeeklyPattern(habit, calendar);
                
            case CUSTOM:
                int interval = habit.getCustomIntervalDays();
                if (interval <= 0) interval = 1;
                calendar.add(Calendar.DAY_OF_YEAR, interval);
                break;
        }
        
        return calendar.getTimeInMillis();
    }

    /**
     * Create a PendingIntent for a habit reminder.
     */
    private PendingIntent createPendingIntent(Habit habit) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_HABIT_ID, habit.getId());
        intent.putExtra(EXTRA_HABIT_NAME, habit.getName());
        intent.putExtra(EXTRA_HABIT_CATEGORY, habit.getCategory());
        
        return PendingIntent.getBroadcast(
                context,
                habit.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Create a PendingIntent for a habit ID (used for cancellation).
     */
    private PendingIntent createPendingIntentForId(int habitId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                habitId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Schedule the end-of-day reminder for high priority habits.
     * Requirements: 9.5 - Send additional reminder for incomplete high priority habits near end of day
     */
    public void scheduleEndOfDayReminder() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, END_OF_DAY_REMINDER_HOUR);
        calendar.set(Calendar.MINUTE, END_OF_DAY_REMINDER_MINUTE);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        PendingIntent pendingIntent = createEndOfDayPendingIntent();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
            Log.d(TAG, "Scheduled end-of-day reminder at " + calendar.getTime());
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to schedule end-of-day reminder - permission denied", e);
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    /**
     * Cancel the end-of-day reminder.
     */
    public void cancelEndOfDayReminder() {
        PendingIntent pendingIntent = createEndOfDayPendingIntent();
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.d(TAG, "Cancelled end-of-day reminder");
    }

    /**
     * Create a PendingIntent for the end-of-day reminder.
     */
    private PendingIntent createEndOfDayPendingIntent() {
        Intent intent = new Intent(context, EndOfDayReminderReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                END_OF_DAY_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
