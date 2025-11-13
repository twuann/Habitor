package com.example.habitor.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habitor.R;
import com.example.habitor.adapter.HabitGridAdapter;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private HabitGridAdapter adapter;
    private List<Habit> habitList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.recyclerHabits);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Load habits từ database
        AppDatabase db = AppDatabase.getInstance(requireContext());
        habitList = db.habitDao().getAll();

        // Nếu chưa có data, tạo sẵn vài mẫu
        if (habitList.isEmpty()) {
            db.habitDao().insert(new Habit("Drink Water"));
            db.habitDao().insert(new Habit("Play football"));
            db.habitDao().insert(new Habit("Go to the Gym"));
            db.habitDao().insert(new Habit("Learn English"));
            db.habitDao().insert(new Habit("Do homework"));
            db.habitDao().insert(new Habit("Read a book"));
            habitList = db.habitDao().getAll();
        }

        adapter = new HabitGridAdapter(requireContext(), habitList);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh danh sách khi người dùng quay lại Home
        AppDatabase db = AppDatabase.getInstance(requireContext());
        habitList.clear();
        habitList.addAll(db.habitDao().getAll());
        adapter.notifyDataSetChanged();
    }
}
