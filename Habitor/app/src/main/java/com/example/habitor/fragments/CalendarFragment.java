package com.example.habitor.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.habitor.R;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.HabitHistory;
import java.text.SimpleDateFormat;
import java.util.*;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvStreak;
    private Button btnMarkDone;
    private String selectedDate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        tvStreak = view.findViewById(R.id.tvStreak);
        btnMarkDone = view.findViewById(R.id.btnMarkDone);

        AppDatabase db = AppDatabase.getInstance(requireContext());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date());

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
        });

        btnMarkDone.setOnClickListener(v -> {
            HabitHistory history = new HabitHistory();
            history.habitName = "General Habit"; // Có thể mở rộng chọn habit cụ thể
            history.date = selectedDate;

            db.habitDao().insertHistory(history);
            Toast.makeText(getContext(), "Marked as done for " + selectedDate, Toast.LENGTH_SHORT).show();
            updateStreak(db);
        });

        updateStreak(db);
        return view;
    }

    private void updateStreak(AppDatabase db) {
        List<String> dates = db.habitDao().getAllDates();
        if (dates.isEmpty()) {
            tvStreak.setText("Current Streak: 0 days");
            return;
        }

        // Tính streak liên tục theo ngày gần nhất
        Collections.sort(dates);
        int streak = 1;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (int i = dates.size() - 1; i > 0; i--) {
                Date d1 = sdf.parse(dates.get(i));
                Date d2 = sdf.parse(dates.get(i - 1));
                long diff = (d1.getTime() - d2.getTime()) / (1000 * 60 * 60 * 24);
                if (diff == 1) {
                    streak++;
                } else break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        tvStreak.setText("Current Streak: " + streak + " days");
    }
}
