package com.example.habitor.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habitor.R;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;

import java.io.File;
import java.io.InputStream;
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

        // Load habit image thumbnail (Requirements: 2.5)
        loadHabitImage(holder.ivHabitImage, habit);

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

    /**
     * Load habit image thumbnail from imagePath.
     * Shows the image if available, hides the ImageView if no image.
     * Requirements: 2.5
     */
    private void loadHabitImage(ImageView imageView, Habit habit) {
        if (habit.hasImage()) {
            String imagePath = habit.getImagePath();
            Bitmap bitmap = null;
            
            try {
                if (imagePath.startsWith("content://")) {
                    // Load from content URI
                    Uri uri = Uri.parse(imagePath);
                    InputStream inputStream = context.getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        bitmap = BitmapFactory.decodeStream(inputStream);
                        inputStream.close();
                    }
                } else {
                    // Load from file path
                    File file = new File(imagePath);
                    if (file.exists()) {
                        bitmap = BitmapFactory.decodeFile(imagePath);
                    }
                }
            } catch (Exception e) {
                // Image loading failed, will show placeholder or hide
                bitmap = null;
            }
            
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            } else {
                // Image file not found or failed to load, hide the ImageView
                imageView.setImageDrawable(null);
                imageView.setVisibility(View.GONE);
            }
        } else {
            // No image attached, hide the ImageView
            imageView.setImageDrawable(null);
            imageView.setVisibility(View.GONE);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivHabitImage;
        TextView tvHabitItemName;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHabitImage = itemView.findViewById(R.id.ivHabitImage);
            tvHabitItemName = itemView.findViewById(R.id.tvHabitItemName);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
