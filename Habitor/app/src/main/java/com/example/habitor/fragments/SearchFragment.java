package com.example.habitor.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habitor.R;
import com.example.habitor.adapter.HabitListAdapter;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import java.util.List;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText edtHabitName;
    private Button btnAddHabit;
    private HabitListAdapter adapter;
    private List<Habit> habitList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        recyclerView = view.findViewById(R.id.recyclerAllHabits);
        edtHabitName = view.findViewById(R.id.edtHabitName);
        btnAddHabit = view.findViewById(R.id.btnAddHabit);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        AppDatabase db = AppDatabase.getInstance(requireContext());
        habitList = db.habitDao().getAll();
        adapter = new HabitListAdapter(requireContext(), habitList);
        recyclerView.setAdapter(adapter);

        btnAddHabit.setOnClickListener(v -> {
            String habitName = edtHabitName.getText().toString().trim();
            if (habitName.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a habit", Toast.LENGTH_SHORT).show();
                return;
            }

            db.habitDao().insert(new Habit(habitName));
            habitList.clear();
            habitList.addAll(db.habitDao().getAll());
            adapter.notifyDataSetChanged();

            edtHabitName.setText("");
            Toast.makeText(getContext(), "Habit added!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}
