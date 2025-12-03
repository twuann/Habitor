package com.example.habitor.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.habitor.R;
import com.example.habitor.utils.AlarmReceiver;
import java.util.Calendar;

/**
 * @deprecated This fragment has been replaced by {@link SettingsFragment} which provides
 * a unified settings experience combining Account, Profile, and Notification settings.
 * This class is kept for reference only and should not be used in new code.
 * 
 * <p>Migration: Use {@link SettingsFragment} instead, which includes all notification
 * settings functionality in the Notifications section.</p>
 * 
 * @see SettingsFragment
 * @see <a href=".kiro/specs/drawer-auth-sync/requirements.md">Requirements 4.2</a>
 */
@Deprecated
public class NotifSettingsFragment extends Fragment {

    private TimePicker timePicker;
    private Switch switchEnableNotif;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notif_settings, container, false);

        timePicker = view.findViewById(R.id.timePicker);
        switchEnableNotif = view.findViewById(R.id.switchEnableNotif);

        alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        switchEnableNotif.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setDailyReminder();
            } else {
                cancelDailyReminder();
            }
        });

        return view;
    }

    private void setDailyReminder() {
        Calendar calendar = Calendar.getInstance();
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );

        Toast.makeText(getContext(), "Daily reminder set for " + hour + ":" + String.format("%02d", minute), Toast.LENGTH_SHORT).show();
    }

    private void cancelDailyReminder() {
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Toast.makeText(getContext(), "Reminder disabled", Toast.LENGTH_SHORT).show();
        }
    }
}
