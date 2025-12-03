package com.example.habitor.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.habitor.R;
import com.example.habitor.activities.MainActivity;
import com.example.habitor.model.Habit;

import java.util.List;

/**
 * NotificationHelper manages habit reminder notifications with action buttons.
 * 
 * Requirements:
 * - 6.1: Include action buttons for "Mark Complete" and "Snooze"
 * - 6.2: Record habit completion when "Mark Complete" is tapped
 * - 6.3: Reschedule reminder for 10 minutes when "Snooze" is tapped
 * - 6.5: Group notifications with summary for multiple reminders
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "habitor_channel_id";
    private static final String CHANNEL_NAME = "Habitor Notifications";
    private static final String GROUP_KEY = "com.example.habitor.HABIT_REMINDERS";
    
    public static final String ACTION_MARK_COMPLETE = "com.example.habitor.ACTION_MARK_COMPLETE";
    public static final String ACTION_SNOOZE = "com.example.habitor.ACTION_SNOOZE";
    public static final String EXTRA_HABIT_ID = "habit_id";
    public static final String EXTRA_HABIT_NAME = "habit_name";
    
    private static final int SUMMARY_NOTIFICATION_ID = 0;

    /**
     * Show a basic notification (legacy method for backward compatibility).
     */
    public static void showNotification(Context context, String title, String message) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(manager);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        manager.notify(1, builder.build());
    }


    /**
     * Show a habit reminder notification with action buttons.
     * Requirements: 6.1 - Include action buttons for "Mark Complete" and "Snooze"
     * 
     * @param context The application context
     * @param habitId The ID of the habit
     * @param habitName The name of the habit
     * @param category The category of the habit
     */
    public static void showHabitReminder(Context context, int habitId, String habitName, String category) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(manager);

        // Create intent to open habit detail when notification body is tapped
        PendingIntent contentIntent = createOpenDetailIntent(context, habitId);
        
        // Create action intents
        PendingIntent markCompleteIntent = createMarkCompleteIntent(context, habitId, habitName);
        PendingIntent snoozeIntent = createSnoozeIntent(context, habitId, habitName);

        String motivationalMessage = getMotivationalMessage(category);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🌿 " + habitName)
                .setContentText(motivationalMessage)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(motivationalMessage + "\n\nTap to view details or use actions below."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setGroup(GROUP_KEY)
                .addAction(R.drawable.ic_launcher_foreground, "✓ Complete", markCompleteIntent)
                .addAction(R.drawable.ic_launcher_foreground, "⏰ Snooze", snoozeIntent);

        manager.notify(habitId, builder.build());
    }

    /**
     * Show a habit reminder notification using a Habit object.
     */
    public static void showHabitReminder(Context context, Habit habit) {
        showHabitReminder(context, habit.getId(), habit.getName(), habit.getCategory());
    }

    /**
     * Show grouped notifications for multiple habit reminders.
     * Requirements: 6.5 - Group notifications with summary for multiple reminders
     * 
     * @param context The application context
     * @param habits List of habits with pending reminders
     */
    public static void showGroupedNotifications(Context context, List<Habit> habits) {
        if (habits == null || habits.isEmpty()) {
            return;
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(manager);

        // Show individual notifications for each habit
        for (Habit habit : habits) {
            showHabitReminder(context, habit);
        }

        // Show summary notification for the group
        if (habits.size() > 1) {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                    .setBigContentTitle(habits.size() + " habits pending");
            
            for (Habit habit : habits) {
                inboxStyle.addLine("• " + habit.getName());
            }

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, SUMMARY_NOTIFICATION_ID, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Habitor Reminders")
                    .setContentText(habits.size() + " habits need your attention")
                    .setStyle(inboxStyle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setGroup(GROUP_KEY)
                    .setGroupSummary(true);

            manager.notify(SUMMARY_NOTIFICATION_ID, summaryBuilder.build());
        }
    }

    /**
     * Show a high priority reminder notification.
     * Used for end-of-day reminders for incomplete high priority habits.
     * 
     * @param context The application context
     * @param habit The high priority habit
     */
    public static void showHighPriorityReminder(Context context, Habit habit) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(manager);

        PendingIntent contentIntent = createOpenDetailIntent(context, habit.getId());
        PendingIntent markCompleteIntent = createMarkCompleteIntent(context, habit.getId(), habit.getName());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🔴 High Priority: " + habit.getName())
                .setContentText("Don't forget to complete this important habit today!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("This is a high priority habit that hasn't been completed yet. " +
                                "Take a moment to work on it before the day ends!"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_launcher_foreground, "✓ Mark Complete", markCompleteIntent);

        // Use a unique ID for high priority notifications (offset by 10000)
        manager.notify(habit.getId() + 10000, builder.build());
    }


    /**
     * Create a PendingIntent for the "Mark Complete" action.
     * Requirements: 6.2 - Record habit completion when tapped
     * 
     * @param context The application context
     * @param habitId The ID of the habit
     * @param habitName The name of the habit
     * @return PendingIntent for mark complete action
     */
    public static PendingIntent createMarkCompleteIntent(Context context, int habitId, String habitName) {
        Intent intent = new Intent(context, NotificationActionReceiver.class);
        intent.setAction(ACTION_MARK_COMPLETE);
        intent.putExtra(EXTRA_HABIT_ID, habitId);
        intent.putExtra(EXTRA_HABIT_NAME, habitName);
        
        return PendingIntent.getBroadcast(
                context,
                habitId * 2, // Unique request code for mark complete
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Create a PendingIntent for the "Snooze" action.
     * Requirements: 6.3 - Reschedule reminder for 10 minutes
     * 
     * @param context The application context
     * @param habitId The ID of the habit
     * @param habitName The name of the habit
     * @return PendingIntent for snooze action
     */
    public static PendingIntent createSnoozeIntent(Context context, int habitId, String habitName) {
        Intent intent = new Intent(context, NotificationActionReceiver.class);
        intent.setAction(ACTION_SNOOZE);
        intent.putExtra(EXTRA_HABIT_ID, habitId);
        intent.putExtra(EXTRA_HABIT_NAME, habitName);
        
        return PendingIntent.getBroadcast(
                context,
                habitId * 2 + 1, // Unique request code for snooze
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Create a PendingIntent to open the habit detail screen.
     * Requirements: 6.4 - Open app and navigate to habit detail when notification body is tapped
     * 
     * @param context The application context
     * @param habitId The ID of the habit
     * @return PendingIntent to open habit detail
     */
    public static PendingIntent createOpenDetailIntent(Context context, int habitId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_HABIT_ID, habitId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        return PendingIntent.getActivity(
                context,
                habitId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Create the notification channel for Android 8+.
     */
    private static void createNotificationChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for daily habits");
            channel.enableVibration(true);
            channel.setShowBadge(true);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Get a motivational message based on the habit category.
     */
    private static String getMotivationalMessage(String category) {
        if (category == null) {
            return "Time to work on your habit! 💪";
        }
        
        switch (category) {
            case "Health":
                return "Your health matters! Take care of yourself today. 🏃‍♂️";
            case "Work":
                return "Stay productive! You've got this. 💼";
            case "Personal":
                return "Personal growth starts with small steps. 🌱";
            case "Learning":
                return "Keep learning, keep growing! 📚";
            default:
                return "Time to work on your habit! 💪";
        }
    }

    /**
     * Cancel a notification by habit ID.
     * 
     * @param context The application context
     * @param habitId The ID of the habit
     */
    public static void cancelNotification(Context context, int habitId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(habitId);
    }

    /**
     * Cancel all notifications.
     * 
     * @param context The application context
     */
    public static void cancelAllNotifications(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
    }
}
