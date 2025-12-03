package com.example.habitor.fragments;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

import com.example.habitor.R;
import com.example.habitor.model.Category;
import com.example.habitor.model.Habit;
import com.example.habitor.model.LocationTriggerType;
import com.example.habitor.model.Priority;
import com.example.habitor.model.RepeatPattern;
import com.example.habitor.repository.HabitRepository;
import com.example.habitor.utils.AlarmScheduler;
import com.example.habitor.utils.LocationHelper;
import com.example.habitor.utils.LocationPermissionHandler;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bottom sheet dialog for adding or editing habits.
 * Requirements: 4.1, 5.1, 9.1, 10.1
 */
public class AddEditHabitBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_HABIT_ID = "habit_id";

    // Mode: -1 = add new, >= 0 = edit existing
    private int habitId = -1;
    private Habit existingHabit;
    private HabitRepository repository;
    private AlarmScheduler alarmScheduler;

    // UI Elements
    private TextView tvTitle;
    private EditText etHabitName;
    private EditText etNote;
    private MaterialButton btnPriorityHigh;
    private MaterialButton btnPriorityMedium;
    private MaterialButton btnPriorityLow;
    private Spinner spinnerCategory;
    private SwitchCompat switchReminder;
    private View layoutTimePicker;
    private TextView tvSelectedTime;
    private TextView tvRepeatPatternLabel;
    private RadioGroup radioGroupPattern;
    private View layoutWeeklyDays;
    private View layoutCustomInterval;
    private EditText etIntervalDays;
    private MaterialButton btnCancel;
    private MaterialButton btnSave;
    private ToggleButton[] dayToggles;
    
    // Location UI Elements
    private View layoutLocation;
    private TextView tvLocationName;
    private TextView tvLocationCoords;
    private TextView tvGetLocation;
    private View layoutLocationReminder;
    private SwitchCompat switchLocationReminder;
    private RadioGroup radioGroupTrigger;
    private CardView cardMapPreview;
    private WebView webViewMapPreview;

    // State
    private Priority selectedPriority = Priority.MEDIUM;
    private Double selectedLatitude = null;
    private Double selectedLongitude = null;
    private String selectedLocationName = null;
    private LocationHelper locationHelper;
    private LocationPermissionHandler permissionHandler;
    private int selectedHour = 8;
    private int selectedMinute = 0;
    private OnHabitSavedListener listener;

    public interface OnHabitSavedListener {
        void onHabitSaved(Habit habit);
    }

    /**
     * Create a new instance for adding a habit.
     */
    public static AddEditHabitBottomSheet newInstance() {
        return new AddEditHabitBottomSheet();
    }

    /**
     * Create a new instance for editing an existing habit.
     */
    public static AddEditHabitBottomSheet newInstance(int habitId) {
        AddEditHabitBottomSheet fragment = new AddEditHabitBottomSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_HABIT_ID, habitId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnHabitSavedListener(OnHabitSavedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            habitId = getArguments().getInt(ARG_HABIT_ID, -1);
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_add_edit_habit, container, false);

        initViews(view);
        initRepository();
        setupCategorySpinner();
        setupListeners();

        if (habitId >= 0) {
            loadExistingHabit();
        }

        return view;
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tvTitle);
        etHabitName = view.findViewById(R.id.etHabitName);
        etNote = view.findViewById(R.id.etNote);
        btnPriorityHigh = view.findViewById(R.id.btnPriorityHigh);
        btnPriorityMedium = view.findViewById(R.id.btnPriorityMedium);
        btnPriorityLow = view.findViewById(R.id.btnPriorityLow);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        switchReminder = view.findViewById(R.id.switchReminder);
        layoutTimePicker = view.findViewById(R.id.layoutTimePicker);
        tvSelectedTime = view.findViewById(R.id.tvSelectedTime);
        tvRepeatPatternLabel = view.findViewById(R.id.tvRepeatPatternLabel);
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

        // Location views
        layoutLocation = view.findViewById(R.id.layoutLocation);
        tvLocationName = view.findViewById(R.id.tvLocationName);
        tvLocationCoords = view.findViewById(R.id.tvLocationCoords);
        tvGetLocation = view.findViewById(R.id.tvGetLocation);
        layoutLocationReminder = view.findViewById(R.id.layoutLocationReminder);
        switchLocationReminder = view.findViewById(R.id.switchLocationReminder);
        radioGroupTrigger = view.findViewById(R.id.radioGroupTrigger);
        cardMapPreview = view.findViewById(R.id.cardMapPreview);
        webViewMapPreview = view.findViewById(R.id.webViewMapPreview);

        // Set default time display
        updateTimeDisplay();

        // Set default priority selection
        selectPriority(Priority.MEDIUM);
    }

    private void initRepository() {
        repository = new HabitRepository(requireContext());
        alarmScheduler = new AlarmScheduler(requireContext());
        locationHelper = new LocationHelper(requireContext());
        if (getActivity() != null) {
            permissionHandler = new LocationPermissionHandler(getActivity());
        }
    }

    private void setupCategorySpinner() {
        String[] categories = {
            Category.CATEGORY_HEALTH,
            Category.CATEGORY_WORK,
            Category.CATEGORY_PERSONAL,
            Category.CATEGORY_LEARNING,
            Category.CATEGORY_OTHER
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Default to "Other"
        spinnerCategory.setSelection(4);
    }

    private void setupListeners() {
        // Priority buttons
        btnPriorityHigh.setOnClickListener(v -> selectPriority(Priority.HIGH));
        btnPriorityMedium.setOnClickListener(v -> selectPriority(Priority.MEDIUM));
        btnPriorityLow.setOnClickListener(v -> selectPriority(Priority.LOW));

        // Reminder toggle
        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateReminderVisibility(isChecked);
        });

        // Time picker
        layoutTimePicker.setOnClickListener(v -> showTimePicker());

        // Repeat pattern radio group
        radioGroupPattern.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioDaily) {
                updatePatternVisibility(RepeatPattern.DAILY);
            } else if (checkedId == R.id.radioWeekly) {
                updatePatternVisibility(RepeatPattern.WEEKLY);
            } else if (checkedId == R.id.radioCustom) {
                updatePatternVisibility(RepeatPattern.CUSTOM);
            }
        });

        // Action buttons
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveHabit());
        
        // Location
        layoutLocation.setOnClickListener(v -> getCurrentLocation());
        tvGetLocation.setOnClickListener(v -> getCurrentLocation());
        
        // Location reminder toggle
        switchLocationReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            radioGroupTrigger.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }
    
    private void getCurrentLocation() {
        if (permissionHandler == null || getActivity() == null) return;
        
        permissionHandler.checkAndRequestForegroundPermission(new LocationPermissionHandler.PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                tvGetLocation.setText("Loading...");
                locationHelper.getCurrentLocation(new LocationHelper.LocationResultCallback() {
                    @Override
                    public void onLocationResult(android.location.Location location) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            selectedLatitude = location.getLatitude();
                            selectedLongitude = location.getLongitude();
                            
                            // Reverse geocode to get address
                            locationHelper.reverseGeocode(selectedLatitude, selectedLongitude, 
                                    new LocationHelper.GeocodingCallback() {
                                @Override
                                public void onAddressResult(String address) {
                                    if (getActivity() == null) return;
                                    getActivity().runOnUiThread(() -> {
                                        selectedLocationName = address;
                                        updateLocationDisplay();
                                    });
                                }

                                @Override
                                public void onGeocodingError(String error) {
                                    if (getActivity() == null) return;
                                    getActivity().runOnUiThread(() -> {
                                        selectedLocationName = "Current Location";
                                        updateLocationDisplay();
                                    });
                                }
                            });
                        });
                    }

                    @Override
                    public void onLocationError(String error) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            tvGetLocation.setText("Get Location");
                            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onPermissionDenied() {
                Toast.makeText(getContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateLocationDisplay() {
        if (selectedLatitude != null && selectedLongitude != null) {
            tvLocationName.setText(selectedLocationName != null ? selectedLocationName : "Current Location");
            tvLocationCoords.setText(String.format(Locale.getDefault(), "%.6f, %.6f", selectedLatitude, selectedLongitude));
            tvLocationCoords.setVisibility(View.VISIBLE);
            tvGetLocation.setText("Update");
            layoutLocationReminder.setVisibility(View.VISIBLE);
            
            // Show and load map preview
            cardMapPreview.setVisibility(View.VISIBLE);
            loadMapPreview();
        } else {
            tvLocationName.setText("No location set");
            tvLocationCoords.setVisibility(View.GONE);
            tvGetLocation.setText("Get Location");
            layoutLocationReminder.setVisibility(View.GONE);
            cardMapPreview.setVisibility(View.GONE);
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void loadMapPreview() {
        if (selectedLatitude == null || selectedLongitude == null) return;
        
        WebSettings webSettings = webViewMapPreview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webViewMapPreview.setWebViewClient(new WebViewClient());
        
        String html = createMapHtml(selectedLatitude, selectedLongitude, selectedLocationName);
        webViewMapPreview.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null);
    }
    
    private String createMapHtml(double lat, double lng, String name) {
        return "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>html,body,#map{height:100%;margin:0;padding:0;}</style>" +
                "</head><body>" +
                "<div id='map'></div>" +
                "<script>" +
                "var map = L.map('map', {zoomControl: false, attributionControl: false}).setView([" + lat + "," + lng + "], 15);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);" +
                "L.marker([" + lat + "," + lng + "]).addTo(map)" +
                ".bindPopup('" + (name != null ? name.replace("'", "\\'") : "Location") + "').openPopup();" +
                "</script></body></html>";
    }


    private void loadExistingHabit() {
        existingHabit = repository.getHabitById(habitId);
        if (existingHabit == null) {
            dismiss();
            return;
        }

        // Update title
        tvTitle.setText("Edit Habit");

        // Populate fields
        etHabitName.setText(existingHabit.getName());
        etNote.setText(existingHabit.getNote());

        // Set priority
        selectPriority(existingHabit.getPriorityEnum());

        // Set category
        String category = existingHabit.getCategory();
        if (category != null) {
            String[] categories = {
                Category.CATEGORY_HEALTH,
                Category.CATEGORY_WORK,
                Category.CATEGORY_PERSONAL,
                Category.CATEGORY_LEARNING,
                Category.CATEGORY_OTHER
            };
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(category)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        // Set reminder settings
        switchReminder.setChecked(existingHabit.isReminderEnabled());
        updateReminderVisibility(existingHabit.isReminderEnabled());

        // Set time
        if (existingHabit.getReminderTime() != null && existingHabit.getReminderTime().contains(":")) {
            String[] parts = existingHabit.getReminderTime().split(":");
            selectedHour = Integer.parseInt(parts[0]);
            selectedMinute = Integer.parseInt(parts[1]);
            updateTimeDisplay();
        }

        // Set repeat pattern
        RepeatPattern pattern = existingHabit.getRepeatPatternEnum();
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
                etIntervalDays.setText(String.valueOf(existingHabit.getCustomIntervalDays()));
                break;
        }
        updatePatternVisibility(pattern);
        
        // Load location data
        if (existingHabit.hasLocation()) {
            selectedLatitude = existingHabit.getLatitude();
            selectedLongitude = existingHabit.getLongitude();
            selectedLocationName = existingHabit.getLocationName();
            updateLocationDisplay();
            
            switchLocationReminder.setChecked(existingHabit.isLocationReminderEnabled());
            if (existingHabit.isLocationReminderEnabled()) {
                radioGroupTrigger.setVisibility(View.VISIBLE);
                if (existingHabit.getLocationTriggerTypeEnum() == LocationTriggerType.EXIT) {
                    radioGroupTrigger.check(R.id.radioExit);
                } else {
                    radioGroupTrigger.check(R.id.radioEnter);
                }
            }
        }
    }

    private void loadWeeklyDays() {
        String repeatDaysJson = existingHabit.getRepeatDays();
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

    private void selectPriority(Priority priority) {
        selectedPriority = priority;

        // Reset all buttons to outlined style
        btnPriorityHigh.setStrokeWidth(2);
        btnPriorityMedium.setStrokeWidth(2);
        btnPriorityLow.setStrokeWidth(2);
        
        // Reset background tint to transparent
        btnPriorityHigh.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        btnPriorityMedium.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        btnPriorityLow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));

        // Highlight selected button with background color and thicker stroke
        switch (priority) {
            case HIGH:
                btnPriorityHigh.setStrokeWidth(6);
                btnPriorityHigh.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x33FF5252)); // Light red
                break;
            case LOW:
                btnPriorityLow.setStrokeWidth(6);
                btnPriorityLow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x3369F0AE)); // Light green
                break;
            default:
                btnPriorityMedium.setStrokeWidth(6);
                btnPriorityMedium.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0x33FFD740)); // Light yellow
                break;
        }
    }

    private void updateReminderVisibility(boolean enabled) {
        int visibility = enabled ? View.VISIBLE : View.GONE;
        layoutTimePicker.setVisibility(visibility);
        tvRepeatPatternLabel.setVisibility(visibility);
        radioGroupPattern.setVisibility(visibility);

        // Also update pattern-specific views
        if (enabled) {
            int checkedId = radioGroupPattern.getCheckedRadioButtonId();
            if (checkedId == R.id.radioWeekly) {
                layoutWeeklyDays.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.radioCustom) {
                layoutCustomInterval.setVisibility(View.VISIBLE);
            }
        } else {
            layoutWeeklyDays.setVisibility(View.GONE);
            layoutCustomInterval.setVisibility(View.GONE);
        }
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


    private void saveHabit() {
        // Validate input
        String name = etHabitName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a habit name", Toast.LENGTH_SHORT).show();
            etHabitName.requestFocus();
            return;
        }

        // Validate weekly days if weekly pattern selected
        boolean isReminderEnabled = switchReminder.isChecked();
        RepeatPattern pattern = getSelectedPattern();
        if (isReminderEnabled && pattern == RepeatPattern.WEEKLY) {
            String repeatDaysJson = getSelectedDaysJson();
            if (repeatDaysJson.equals("[]")) {
                Toast.makeText(getContext(), "Please select at least one day", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create or update habit
        Habit habit;
        if (existingHabit != null) {
            habit = existingHabit;
        } else {
            habit = new Habit(name);
        }

        // Set basic fields
        habit.setName(name);
        habit.setNote(etNote.getText().toString().trim());
        habit.setPriorityEnum(selectedPriority);
        habit.setCategory((String) spinnerCategory.getSelectedItem());

        // Set reminder fields
        habit.setReminderEnabled(isReminderEnabled);
        if (isReminderEnabled) {
            habit.setReminderTime(String.format(Locale.getDefault(), "%02d:%02d",
                selectedHour, selectedMinute));
            habit.setRepeatPatternEnum(pattern);
            habit.setRepeatDays(getSelectedDaysJson());
            habit.setCustomIntervalDays(getCustomInterval());
        }
        
        // Set location fields
        habit.setLatitude(selectedLatitude);
        habit.setLongitude(selectedLongitude);
        habit.setLocationName(selectedLocationName);
        habit.setLocationReminderEnabled(switchLocationReminder.isChecked() && selectedLatitude != null);
        if (radioGroupTrigger.getCheckedRadioButtonId() == R.id.radioExit) {
            habit.setLocationTriggerTypeEnum(LocationTriggerType.EXIT);
        } else {
            habit.setLocationTriggerTypeEnum(LocationTriggerType.ENTER);
        }

        // Save to repository
        if (existingHabit != null) {
            // Update existing habit
            repository.updateHabit(habit, new HabitRepository.OnCompleteCallback() {
                @Override
                public void onSuccess() {
                    handleSaveSuccess(habit);
                }

                @Override
                public void onFailure(Exception e) {
                    handleSaveFailure(e);
                }
            });
        } else {
            // Insert new habit
            repository.insertHabit(habit, new HabitRepository.OnHabitInsertCallback() {
                @Override
                public void onSuccess(int habitId) {
                    habit.setId(habitId);
                    handleSaveSuccess(habit);
                }

                @Override
                public void onFailure(Exception e) {
                    handleSaveFailure(e);
                }
            });
        }
    }

    private void handleSaveSuccess(Habit habit) {
        // Run on UI thread since callback may come from background thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Schedule or cancel alarm based on reminder settings
                if (habit.isReminderEnabled()) {
                    alarmScheduler.scheduleReminder(habit);
                    Toast.makeText(getContext(), "Habit saved with reminder", Toast.LENGTH_SHORT).show();
                } else {
                    alarmScheduler.cancelReminder(habit.getId());
                    Toast.makeText(getContext(), "Habit saved", Toast.LENGTH_SHORT).show();
                }

                // Notify listener
                if (listener != null) {
                    listener.onHabitSaved(habit);
                }

                dismiss();
            });
        }
    }

    private void handleSaveFailure(Exception e) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Failed to save habit: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
        }
    }

    private RepeatPattern getSelectedPattern() {
        int checkedId = radioGroupPattern.getCheckedRadioButtonId();
        if (checkedId == R.id.radioWeekly) {
            return RepeatPattern.WEEKLY;
        } else if (checkedId == R.id.radioCustom) {
            return RepeatPattern.CUSTOM;
        }
        return RepeatPattern.DAILY;
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

    private int getCustomInterval() {
        try {
            int interval = Integer.parseInt(etIntervalDays.getText().toString());
            return interval > 0 ? interval : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
