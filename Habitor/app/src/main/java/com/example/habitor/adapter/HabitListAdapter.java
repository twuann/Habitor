package com.example.habitor.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habitor.R;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import java.util.List;

public class HabitListAdapter extends RecyclerView.Adapter<HabitListAdapter.ViewHolder> {

    private Context context;
    private List<Habit> habitList;

    public HabitListAdapter(Context context, List<Habit> habitList) {
        this.context = context;
        this.habitList = habitList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_habit_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Habit habit = habitList.get(position);
        holder.tvHabitItemName.setText(habit.name);

        holder.btnDelete.setOnClickListener(v -> {
            AppDatabase db = AppDatabase.getInstance(context);
            db.habitDao().moveToTrash(habit.id);
            habitList.remove(position);
            notifyItemRemoved(position);
            Toast.makeText(context, "Moved to Trash", Toast.LENGTH_SHORT).show();
        });

        holder.btnEdit.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Edit Habit");

            final EditText input = new EditText(context);
            input.setText(habit.name);
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    habit.name = newName;
                    AppDatabase db = AppDatabase.getInstance(context);
                    db.habitDao().insert(habit); // đơn giản: ghi đè lại
                    notifyItemChanged(position);
                    Toast.makeText(context, "Updated!", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHabitItemName;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHabitItemName = itemView.findViewById(R.id.tvHabitItemName);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
