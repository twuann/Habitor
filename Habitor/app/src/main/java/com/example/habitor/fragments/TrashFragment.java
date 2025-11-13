package com.example.habitor.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habitor.R;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import java.util.List;

public class TrashFragment extends Fragment {

    private RecyclerView recyclerView;
    private TrashAdapter adapter;
    private List<Habit> trashList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trash, container, false);
        recyclerView = view.findViewById(R.id.recyclerTrash);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        AppDatabase db = AppDatabase.getInstance(requireContext());
        trashList = db.habitDao().getTrash();

        adapter = new TrashAdapter(trashList, db);
        recyclerView.setAdapter(adapter);

        return view;
    }

    private static class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.ViewHolder> {
        private List<Habit> trashList;
        private AppDatabase db;

        TrashAdapter(List<Habit> trashList, AppDatabase db) {
            this.trashList = trashList;
            this.db = db;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit_trash, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Habit habit = trashList.get(position);
            holder.tvTrashName.setText(habit.name);

            holder.btnRestore.setOnClickListener(v -> {
                db.habitDao().restoreHabit(habit.id);
                trashList.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(v.getContext(), "Habit restored!", Toast.LENGTH_SHORT).show();
            });

            holder.btnDeleteForever.setOnClickListener(v -> {
                db.habitDao().deleteHabit(habit);
                trashList.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(v.getContext(), "Deleted permanently", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return trashList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTrashName;
            Button btnRestore, btnDeleteForever;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTrashName = itemView.findViewById(R.id.tvTrashName);
                btnRestore = itemView.findViewById(R.id.btnRestore);
                btnDeleteForever = itemView.findViewById(R.id.btnDeleteForever);
            }
        }
    }
}
