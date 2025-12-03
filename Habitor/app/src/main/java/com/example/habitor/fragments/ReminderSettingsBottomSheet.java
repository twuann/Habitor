package com.example.habitor.fragments;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.example.habitor.R;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import com.example.habitor.model.HabitDao;
import com.example.habitor.model.RepeatPattern;
import com.example.habitor.utils.AlarmScheduler;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bottom sheet dialog for configuring habit reminder settings.
 * Requirements: 8.3, 5.1, 5.2, 5.3, 5.4
 */
public class ReminderSettingsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_HABIT_ID = "habit_id";

    private int habitId;
    private Habit habit;
    private HabitDao habitDao;

    // UI Elements
    private SwitchCompat switchReminder;
    private View layoutTimePicker;
    private TextView tvSelectedTime;
    private RadioGroup radioGroupPattern;
    private View layoutWeeklyDays;
    private View layoutCustomInterval;
    private EditText etIntervalDays;
    private MaterialButton btnCancel;
    private MaterialButton btnSave;


    // Day toggle buttons
    private ToggleButton[] dayToggles;

    // State
    private int selectedHour = 8;
    private int selectedMinute = 0;
    private OnReminderSavedListener listener;

    public interface OnReminderSavedListener {
        void onReminderSaved();
    }

    public static ReminderSettingsBottomSheet newInstance(int habitId) {
        ReminderSettingsBottomSheet fragment = new ReminderSettingsBottomSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_HABIT_ID, habitId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnReminderSavedListener(OnReminderSavedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            habitId = getArguments().getInt(ARG_HABIT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_reminder_settings, container, false);

        initViews(view);
        initDatabase();
        loadHabitData();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        switchReminder = view.findViewById(R.id.switchReminder);
        layoutTimePicker = view.findViewById(R.id.layoutTimePicker);
        tvSelectedTime = view.findViewById(R.id.tvSelectedTime);
        radioGroupPattern = view.findViewById(R.id.radioGroupPattern);
        layoutWeeklyDays = view.findViewById(R.id.layoutWeeklyDays);
        layoutCustomInterval = view.findViewById(R.id.layoutCustomInterval);
        etIntervalDays = view.findViewById(R.id.etIntervalDays);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);

        // Initialize day toggle buttons
        dayToggles = new ToggleButton[7];
        dayToggles[0] = view.findViewById(R.id.toggleSun);
        dayToggles[1] = view.findViewById(R.id.toggleMon);
        dayToggles[2] = view.findViewById(R.id.toggleTue);
        dayToggles[3] = view.findViewById(R.id.toggleWed);
        dayToggles[4] = view.findViewById(R.id.toggleThu);
        dayToggles[5] = view.findViewById(R.id.toggleFri);
        dayToggles[6] = view.findViewById(R.id.toggleSat);
    }

    private void initDatabase() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        habitDao = db.habitDao();
    }


    private void loadHabitData() {
        habit = habitDao.getHabitById(habitId);
        if (habit == null) {
            dismiss();
            return;
        }

        // Set reminder enabled state
        switchReminder.setChecked(habit.isReminderEnabled());
        updateUIEnabledState(habit.isReminderEnabled());

        // Set time
        if (habit.getReminderTime() != null && habit.getReminderTime().contains(":")) {
            String[] parts = habit.getReminderTime().split(":");
            selectedHour = Integer.parseInt(parts[0]);
            selectedMinute = Integer.parseInt(parts[1]);
        }
        updateTimeDisplay();

        // Set repeat pattern
        RepeatPattern pattern = habit.getRepeatPatternEnum();
        switch (pattern) {
            case DAILY:
                radioGroupPattern.check(R.id.radioDaily);
                break;
            case WEEKLY:
                radioGroupPattern.check(R.id.radioWeekly);
                loadWeeklyDays();
                break;
            case CUSTOM:
                radioGroupPattern.check(R.id.radioCustom);
                etIntervalDays.setText(String.valueOf(habit.getCustomIntervalDays()));
                break;
        }
        updatePatternVisibility(pattern);
    }

    private void loadWeeklyDays() {
        String repeatDaysJson = habit.getRepeatDays();
        if (repeatDaysJson == null || repeatDaysJson.equals("[]")) {
            return;
        }

        try {
            JSONArray array = new JSONArray(repeatDaysJson);
            for (int i = 0; i < array.length(); i++) {
                int dayIndex = array.getInt(i);
                if (dayIndex >= 0 && dayIndex < 7) {
                    dayToggles[dayIndex].setChecked(true);
                }
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
    }

    private void setupListeners() {
        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateUIEnabledState(isChecked);
        });

        layoutTimePicker.setOnClickListener(v -> showTimePicker());

        radioGroupPattern.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioDaily) {
                updatePatternVisibility(RepeatPattern.DAILY);
            } else if (checkedId == R.id.radioWeekly) {
                updatePatternVisibility(RepeatPattern.WEEKLY);
            } else if (checkedId == R.id.radioCustom) {
                updatePatternVisibility(RepeatPattern.CUSTOM);
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveSettings());
    }


    private void updateUIEnabledState(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.5f;
        layoutTimePicker.setAlpha(alpha);
        layoutTimePicker.setEnabled(enabled);
        radioGroupPattern.setAlpha(alpha);
        for (int i = 0; i < radioGroupPattern.getChildCount(); i++) {
            radioGroupPattern.getChildAt(i).setEnabled(enabled);
        }
        layoutWeeklyDays.setAlpha(alpha);
        for (ToggleButton toggle : dayToggles) {
            toggle.setEnabled(enabled);
        }
        layoutCustomInterval.setAlpha(alpha);
        etIntervalDays.setEnabled(enabled);
    }

    private void updatePatternVisibility(RepeatPattern pattern) {
        layoutWeeklyDays.setVisibility(pattern == RepeatPattern.WEEKLY ? View.VISIBLE : View.GONE);
        layoutCustomInterval.setVisibility(pattern == RepeatPattern.CUSTOM ? View.VISIBLE : View.GONE);
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    updateTimeDisplay();
                },
                selectedHour,
                selectedMinute,
                false
        );
        dialog.show();
    }

    private void updateTimeDisplay() {
        String amPm = selectedHour >= 12 ? "PM" : "AM";
        int displayHour = selectedHour % 12;
        if (displayHour == 0) displayHour = 12;
        tvSelectedTime.setText(String.format(Locale.getDefault(), "%d:%02d %s", 
                displayHour, selectedMinute, amPm));
    }

    private void saveSettings() {
        boolean isEnabled = switchReminder.isChecked();
        String reminderTime = String.format(Locale.getDefault(), "%02d:%02d", 
                selectedHour, selectedMinute);

        // Determine pattern
        RepeatPattern pattern;
        int checkedId = radioGroupPattern.getCheckedRadioButtonId();
        if (checkedId == R.id.radioWeekly) {
            pattern = RepeatPattern.WEEKLY;
        } else if (checkedId == R.id.radioCustom) {
            pattern = RepeatPattern.CUSTOM;
        } else {
            pattern = RepeatPattern.DAILY;
        }

        // Get repeat days for weekly pattern
        String repeatDaysJson = "[]";
        if (pattern == RepeatPattern.WEEKLY) {
            repeatDaysJson = getSelectedDaysJson();
            if (repeatDaysJson.equals("[]")) {
                Toast.makeText(getContext(), "Please select at least one day", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Get custom interval
        int customInterval = 1;
        if (pattern == RepeatPattern.CUSTOM) {
            try {
                customInterval = Integer.parseInt(etIntervalDays.getText().toString());
                if (customInterval < 1) customInterval = 1;
            } catch (NumberFormatException e) {
                customInterval = 1;
            }
        }


        // Update database
        habitDao.updateReminderSettings(
                habitId,
                reminderTime,
                isEnabled,
                pattern.name(),
                repeatDaysJson,
                customInterval
        );

        // Update alarm
        habit = habitDao.getHabitById(habitId);
        AlarmScheduler alarmScheduler = new AlarmScheduler(requireContext());
        if (isEnabled) {
            alarmScheduler.scheduleReminder(habit);
            Toast.makeText(getContext(), "Reminder scheduled", Toast.LENGTH_SHORT).show();
        } else {
            alarmScheduler.cancelReminder(habitId);
            Toast.makeText(getContext(), "Reminder disabled", Toast.LENGTH_SHORT).show();
        }

        // Notify listener
        if (listener != null) {
            listener.onReminderSaved();
        }

        dismiss();
    }

    private String getSelectedDaysJson() {
        List<Integer> selectedDays = new ArrayList<>();
        for (int i = 0; i < dayToggles.length; i++) {
            if (dayToggles[i].isChecked()) {
                selectedDays.add(i);
            }
        }

        JSONArray array = new JSONArray();
        for (Integer day : selectedDays) {
            array.put(day);
        }
        return array.toString();
    }
}
