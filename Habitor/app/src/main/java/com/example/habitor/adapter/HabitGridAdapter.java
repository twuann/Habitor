package com.example.habitor.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habitor.R;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;

import java.util.List;

public class HabitGridAdapter extends RecyclerView.Adapter<HabitGridAdapter.ViewHolder> {

    private Context context;
    private List<Habit> habitList;

    public HabitGridAdapter(Context context, List<Habit> habitList) {
        this.context = context;
        this.habitList = habitList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_habit_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Habit habit = habitList.get(position);

        // Hiển thị tên Habit
        holder.tvHabitName.setText(habit.getName());

        // 🔥 Hiển thị NOTE bên dưới
        if (habit.getNote() != null && !habit.getNote().isEmpty()) {
            holder.tvHabitNote.setText(habit.getNote());
        } else {
            holder.tvHabitNote.setText("No note");
        }

        // Click bình thường
        holder.cardView.setOnClickListener(v ->
                Toast.makeText(context, "Clicked " + habit.getName(), Toast.LENGTH_SHORT).show());

        // 🔥 Nhấn giữ để sửa NOTE
        holder.cardView.setOnLongClickListener(v -> {
            showEditNoteDialog(habit);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    // =============================
    // 🔥 HÀM HIỂN THỊ DIALOG CHỈNH NOTE
    // =============================
    private void showEditNoteDialog(Habit habit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Note");

        // Input field
        EditText input = new EditText(context);
        input.setText(habit.getNote());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newNote = input.getText().toString();

            // Cập nhật DB
            AppDatabase.getInstance(context)
                    .habitDao()
                    .updateNote(habit.id, newNote);

            // Cập nhật vào RAM
            habit.setNote(newNote);

            // Refresh UI
            notifyDataSetChanged();

            Toast.makeText(context, "Note updated!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHabitName, tvHabitNote;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = (CardView) itemView;
            tvHabitName = itemView.findViewById(R.id.tvHabitName);
            tvHabitNote = itemView.findViewById(R.id.tvHabitNote); // 🔥 ánh xạ note
        }
    }
}
