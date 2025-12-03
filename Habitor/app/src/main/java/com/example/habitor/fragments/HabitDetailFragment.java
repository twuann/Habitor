package com.example.habitor.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.habitor.model.LocationTriggerType;

import com.example.habitor.R;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import com.example.habitor.model.HabitDao;
import com.example.habitor.model.HabitHistory;
import com.example.habitor.model.Priority;
import com.example.habitor.utils.RepeatPatternFormatter;
import com.example.habitor.utils.StreakCalculator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fragment to display detailed habit information including calendar view and statistics.
 * Requirements: 8.1, 8.2, 8.4, 8.5
 */
public class HabitDetailFragment extends Fragment {

    private static final String ARG_HABIT_ID = "habit_id";

    private int habitId;
    private Habit habit;
    private HabitDao habitDao;
    private List<HabitHistory> habitHistory;
    private Set<String> completedDates;

    // UI Elements
    private TextView tvHabitName;
    private TextView tvPriorityBadge;
    private TextView tvCategory;
    private TextView tvNote;
    private TextView tvCurrentMonth;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private ImageButton btnEditHabit;
    private LinearLayout layoutDayHeaders;
    private GridLayout gridCalendar;
    private TextView tvCurrentStreak;
    private TextView tvLongestStreak;
    private TextView tvCompletionRate;
    private TextView tvReminderTime;
    private TextView tvRepeatPattern;
    private LinearLayout layoutReminderSettings;
    
    // Location UI Elements
    private LinearLayout layoutLocationSection;
    private WebView webViewMap;
    private TextView tvDetailLocationName;
    private TextView tvDetailLocationCoords;
    private TextView tvLocationReminderStatus;


    // Calendar state
    private Calendar displayedMonth;
    private static final String[] DAY_NAMES = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
    private GestureDetector gestureDetector;

    public static HabitDetailFragment newInstance(int habitId) {
        HabitDetailFragment fragment = new HabitDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_HABIT_ID, habitId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            habitId = getArguments().getInt(ARG_HABIT_ID);
        }
        displayedMonth = Calendar.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habit_detail, container, false);

        initViews(view);
        initDatabase();
        loadHabitData();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        tvHabitName = view.findViewById(R.id.tvHabitName);
        tvPriorityBadge = view.findViewById(R.id.tvPriorityBadge);
        tvCategory = view.findViewById(R.id.tvCategory);
        tvNote = view.findViewById(R.id.tvNote);
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        btnEditHabit = view.findViewById(R.id.btnEditHabit);
        layoutDayHeaders = view.findViewById(R.id.layoutDayHeaders);
        gridCalendar = view.findViewById(R.id.gridCalendar);
        tvCurrentStreak = view.findViewById(R.id.tvCurrentStreak);
        tvLongestStreak = view.findViewById(R.id.tvLongestStreak);
        tvCompletionRate = view.findViewById(R.id.tvCompletionRate);
        tvReminderTime = view.findViewById(R.id.tvReminderTime);
        tvRepeatPattern = view.findViewById(R.id.tvRepeatPattern);
        layoutReminderSettings = view.findViewById(R.id.layoutReminderSettings);
        
        // Location views
        layoutLocationSection = view.findViewById(R.id.layoutLocationSection);
        webViewMap = view.findViewById(R.id.webViewMap);
        tvDetailLocationName = view.findViewById(R.id.tvDetailLocationName);
        tvDetailLocationCoords = view.findViewById(R.id.tvDetailLocationCoords);
        tvLocationReminderStatus = view.findViewById(R.id.tvLocationReminderStatus);
    }

    private void initDatabase() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        habitDao = db.habitDao();
    }

    private void loadHabitData() {
        habit = habitDao.getHabitById(habitId);
        if (habit == null) {
            return;
        }

        // Load habit history
        habitHistory = habitDao.getHistoryForHabit(habit.getName());
        completedDates = extractCompletedDates(habitHistory);

        // Update UI
        updateHabitInfo();
        updateCalendar();
        updateStatistics();
        updateReminderSettings();
        updateLocationSection();
    }


    @SuppressLint("ClickableViewAccessibility")
    private void setupListeners() {
        btnPrevMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, 1);
            updateCalendar();
        });

        layoutReminderSettings.setOnClickListener(v -> {
            showReminderSettingsBottomSheet();
        });

        btnEditHabit.setOnClickListener(v -> {
            showEditHabitBottomSheet();
        });

        // Setup swipe gesture for month navigation (Requirement 8.5)
        setupSwipeGesture();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSwipeGesture() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe right - go to previous month
                            displayedMonth.add(Calendar.MONTH, -1);
                        } else {
                            // Swipe left - go to next month
                            displayedMonth.add(Calendar.MONTH, 1);
                        }
                        updateCalendar();
                        return true;
                    }
                }
                return false;
            }
        });

        gridCalendar.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void updateHabitInfo() {
        tvHabitName.setText(habit.getName());

        // Set priority badge
        Priority priority = habit.getPriorityEnum();
        tvPriorityBadge.setText(priority.name());
        GradientDrawable priorityBg = new GradientDrawable();
        priorityBg.setCornerRadius(16);
        switch (priority) {
            case HIGH:
                priorityBg.setColor(getResources().getColor(R.color.priority_high, null));
                break;
            case MEDIUM:
                priorityBg.setColor(getResources().getColor(R.color.priority_medium, null));
                break;
            case LOW:
                priorityBg.setColor(getResources().getColor(R.color.priority_low, null));
                break;
        }
        tvPriorityBadge.setBackground(priorityBg);

        // Set category
        tvCategory.setText(habit.getCategory());

        // Set note
        if (habit.getNote() != null && !habit.getNote().isEmpty()) {
            tvNote.setText(habit.getNote());
            tvNote.setVisibility(View.VISIBLE);
        } else {
            tvNote.setVisibility(View.GONE);
        }
    }

    private void updateCalendar() {
        // Update month title
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvCurrentMonth.setText(monthFormat.format(displayedMonth.getTime()));

        // Setup day headers
        setupDayHeaders();

        // Build calendar grid
        buildCalendarGrid();
    }

    private void setupDayHeaders() {
        layoutDayHeaders.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int cellSize = (int) (40 * density);

        for (String dayName : DAY_NAMES) {
            TextView tv = new TextView(getContext());
            tv.setText(dayName);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(getResources().getColor(R.color.text_secondary, null));
            tv.setTextSize(12);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, cellSize, 1);
            tv.setLayoutParams(params);
            layoutDayHeaders.addView(tv);
        }
    }


    private void buildCalendarGrid() {
        gridCalendar.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int cellSize = (int) (40 * density);

        // Get first day of month
        Calendar cal = (Calendar) displayedMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar today = Calendar.getInstance();

        // Add empty cells for days before first day of month
        for (int i = 0; i < firstDayOfWeek; i++) {
            addEmptyCell(cellSize);
        }

        // Add day cells with streak connection detection
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = dateFormat.format(cal.getTime());
            boolean isCompleted = completedDates.contains(dateStr);
            boolean isToday = isSameDay(cal, today);
            boolean isFuture = cal.after(today);

            // Check for streak connections (Requirement 8.4)
            boolean hasPrevStreak = false;
            boolean hasNextStreak = false;
            
            if (isCompleted) {
                // Check previous day
                Calendar prevCal = (Calendar) cal.clone();
                prevCal.add(Calendar.DAY_OF_MONTH, -1);
                String prevDateStr = dateFormat.format(prevCal.getTime());
                hasPrevStreak = completedDates.contains(prevDateStr);
                
                // Check next day
                Calendar nextCal = (Calendar) cal.clone();
                nextCal.add(Calendar.DAY_OF_MONTH, 1);
                String nextDateStr = dateFormat.format(nextCal.getTime());
                hasNextStreak = completedDates.contains(nextDateStr);
            }

            addDayCell(day, isCompleted, isToday, isFuture, hasPrevStreak, hasNextStreak, cellSize);
        }
    }

    private void addEmptyCell(int cellSize) {
        TextView tv = new TextView(getContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = cellSize;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        tv.setLayoutParams(params);
        gridCalendar.addView(tv);
    }

    private void addDayCell(int day, boolean isCompleted, boolean isToday, boolean isFuture,
                            boolean hasPrevStreak, boolean hasNextStreak, int cellSize) {
        TextView tv = new TextView(getContext());
        tv.setText(String.valueOf(day));
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(14);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = cellSize;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(2, 2, 2, 2);
        tv.setLayoutParams(params);

        // Style based on completion status
        GradientDrawable bg = new GradientDrawable();
        
        // Adjust corner radius based on streak connections (Requirement 8.4)
        float[] radii = new float[8];
        float fullRadius = cellSize / 2f;
        float smallRadius = 8f;
        
        if (isCompleted && (hasPrevStreak || hasNextStreak)) {
            // Create connected appearance for streak days
            // Left corners
            radii[0] = radii[1] = hasPrevStreak ? smallRadius : fullRadius;
            radii[6] = radii[7] = hasPrevStreak ? smallRadius : fullRadius;
            // Right corners
            radii[2] = radii[3] = hasNextStreak ? smallRadius : fullRadius;
            radii[4] = radii[5] = hasNextStreak ? smallRadius : fullRadius;
            bg.setCornerRadii(radii);
        } else {
            bg.setCornerRadius(fullRadius);
        }

        if (isCompleted) {
            bg.setColor(getResources().getColor(R.color.completed_green, null));
            tv.setTextColor(Color.WHITE);
        } else if (isFuture) {
            bg.setColor(Color.TRANSPARENT);
            tv.setTextColor(getResources().getColor(R.color.progress_background, null));
        } else {
            bg.setColor(getResources().getColor(R.color.progress_background, null));
            tv.setTextColor(getResources().getColor(R.color.text_secondary, null));
        }

        // Highlight today
        if (isToday) {
            bg.setStroke(4, getResources().getColor(R.color.primary, null));
        }

        tv.setBackground(bg);
        gridCalendar.addView(tv);
    }


    private void updateStatistics() {
        // Calculate statistics using StreakCalculator (Requirement 8.2)
        int currentStreak = StreakCalculator.calculateCurrentStreak(habitHistory);
        int longestStreak = StreakCalculator.calculateLongestStreak(habitHistory);

        // Calculate total days since first completion or 30 days default
        int totalDays = calculateTotalDays();
        float completionRate = StreakCalculator.calculateCompletionRate(habitHistory, totalDays);

        // Update UI
        tvCurrentStreak.setText(String.valueOf(currentStreak));
        tvLongestStreak.setText(String.valueOf(longestStreak));
        tvCompletionRate.setText(String.format(Locale.getDefault(), "%.0f%%", completionRate));
    }

    private int calculateTotalDays() {
        if (habitHistory == null || habitHistory.isEmpty()) {
            return 30; // Default to 30 days
        }

        // Find earliest date in history
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar earliest = Calendar.getInstance();
        Calendar today = Calendar.getInstance();

        for (HabitHistory h : habitHistory) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(h.date));
                if (cal.before(earliest)) {
                    earliest = cal;
                }
            } catch (Exception e) {
                // Ignore parse errors
            }
        }

        // Calculate days between earliest and today
        long diffMillis = today.getTimeInMillis() - earliest.getTimeInMillis();
        int days = (int) (diffMillis / (1000 * 60 * 60 * 24)) + 1;
        return Math.max(days, 1);
    }

    private void updateReminderSettings() {
        if (habit.isReminderEnabled() && habit.getReminderTime() != null) {
            tvReminderTime.setText(formatTime(habit.getReminderTime()));
            String patternText = RepeatPatternFormatter.formatToReadable(
                    habit.getRepeatPatternEnum(),
                    habit.getRepeatDays(),
                    habit.getCustomIntervalDays()
            );
            tvRepeatPattern.setText(patternText);
        } else {
            tvReminderTime.setText("Not set");
            tvRepeatPattern.setText("Tap to configure");
        }
    }

    private String formatTime(String time24) {
        if (time24 == null || !time24.contains(":")) {
            return time24;
        }
        try {
            String[] parts = time24.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String amPm = hour >= 12 ? "PM" : "AM";
            hour = hour % 12;
            if (hour == 0) hour = 12;
            return String.format(Locale.getDefault(), "%d:%02d %s", hour, minute, amPm);
        } catch (Exception e) {
            return time24;
        }
    }




    @SuppressLint("SetJavaScriptEnabled")
    private void updateLocationSection() {
        if (habit == null || !habit.hasLocation()) {
            layoutLocationSection.setVisibility(View.GONE);
            return;
        }

        layoutLocationSection.setVisibility(View.VISIBLE);
        
        // Set location name and coordinates
        tvDetailLocationName.setText(habit.getLocationName() != null ? 
                habit.getLocationName() : "Location");
        tvDetailLocationCoords.setText(String.format(java.util.Locale.getDefault(), 
                "%.6f, %.6f", habit.getLatitude(), habit.getLongitude()));
        
        // Set reminder status badge
        if (habit.isLocationReminderEnabled()) {
            tvLocationReminderStatus.setVisibility(View.VISIBLE);
            LocationTriggerType triggerType = habit.getLocationTriggerTypeEnum();
            tvLocationReminderStatus.setText(triggerType == LocationTriggerType.ENTER ? "Arrive" : "Leave");
            
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(16);
            bg.setColor(getResources().getColor(
                    triggerType == LocationTriggerType.ENTER ? R.color.completed_green : R.color.priority_medium, null));
            tvLocationReminderStatus.setBackground(bg);
        } else {
            tvLocationReminderStatus.setVisibility(View.GONE);
        }
        
        // Setup WebView with OpenStreetMap
        setupMapWebView();
        
        // Click to open in external maps app
        layoutLocationSection.setOnClickListener(v -> openInMapsApp());
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupMapWebView() {
        if (habit == null || !habit.hasLocation()) return;
        
        WebSettings webSettings = webViewMap.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webViewMap.setWebViewClient(new WebViewClient());
        
        // Create HTML with OpenStreetMap using Leaflet.js
        String html = createMapHtml(habit.getLatitude(), habit.getLongitude(), habit.getLocationName());
        webViewMap.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null);
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
    
    private void openInMapsApp() {
        if (habit == null || !habit.hasLocation()) return;
        
        // Open location in Google Maps or other maps app
        String uri = String.format(java.util.Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                habit.getLatitude(), habit.getLongitude(),
                habit.getLatitude(), habit.getLongitude(),
                habit.getLocationName() != null ? habit.getLocationName() : "Location");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void showReminderSettingsBottomSheet() {
        ReminderSettingsBottomSheet bottomSheet = ReminderSettingsBottomSheet.newInstance(habitId);
        bottomSheet.setOnReminderSavedListener(() -> {
            // Reload habit data when reminder settings are saved
            loadHabitData();
        });
        bottomSheet.show(getParentFragmentManager(), "ReminderSettingsBottomSheet");
    }

    private void showEditHabitBottomSheet() {
        AddEditHabitBottomSheet bottomSheet = AddEditHabitBottomSheet.newInstance(habitId);
        bottomSheet.setOnHabitSavedListener(savedHabit -> {
            // Reload habit data when habit is saved
            loadHabitData();
        });
        bottomSheet.show(getParentFragmentManager(), "EditHabitBottomSheet");
    }

    private Set<String> extractCompletedDates(List<HabitHistory> history) {
        Set<String> dates = new HashSet<>();
        if (history != null) {
            for (HabitHistory h : history) {
                if (h.date != null) {
                    dates.add(h.date);
                }
            }
        }
        return dates;
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        loadHabitData();
    }
}
