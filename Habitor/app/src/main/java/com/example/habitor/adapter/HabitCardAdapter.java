package com.example.habitor.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habitor.R;
import com.example.habitor.model.Habit;
import com.example.habitor.model.Priority;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter for displaying habits in a card-based layout.
 * Requirements: 7.1, 7.2, 9.3
 * - Display habit name, streak, next reminder time
 * - Show priority indicator (colored badge)
 * - Show category label
 * - Add completion checkbox with animation
 */
public class HabitCardAdapter extends RecyclerView.Adapter<HabitCardAdapter.ViewHolder> {

    private final Context context;
    private List<Habit> habitList;
    private final Set<Integer> completedToday;
    private OnHabitInteractionListener listener;

    public interface OnHabitInteractionListener {
        void onHabitClick(Habit habit, int position);
        void onHabitLongClick(Habit habit, int position, View anchorView);
        void onCompletionToggled(Habit habit, int position, boolean isCompleted);
    }

    public HabitCardAdapter(Context context, List<Habit> habitList) {
        this.context = context;
        this.habitList = habitList;
        this.completedToday = new HashSet<>();
    }


    public void setOnHabitInteractionListener(OnHabitInteractionListener listener) {
        this.listener = listener;
    }

    public void setCompletedHabits(Set<Integer> completedIds) {
        this.completedToday.clear();
        if (completedIds != null) {
            this.completedToday.addAll(completedIds);
        }
        notifyDataSetChanged();
    }

    public void updateHabitList(List<Habit> newList) {
        this.habitList = newList;
        notifyDataSetChanged();
    }

    public int getCompletedCount() {
        return completedToday.size();
    }

    public int getTotalCount() {
        return habitList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_habit_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Habit habit = habitList.get(position);
        boolean isCompleted = completedToday.contains(habit.getId());

        // Set habit name
        holder.tvHabitName.setText(habit.getName());

        // Set priority indicator color
        setPriorityIndicator(holder.viewPriorityIndicator, habit.getPriority());

        // Set category label
        setCategoryLabel(holder.tvCategory, habit.getCategory());

        // Set streak count
        int streak = habit.getStreakCount();
        if (streak > 0) {
            holder.tvStreakCount.setText(streak + (streak == 1 ? " day" : " days"));
            holder.tvStreakIcon.setVisibility(View.VISIBLE);
            holder.tvStreakCount.setVisibility(View.VISIBLE);
        } else {
            holder.tvStreakIcon.setVisibility(View.GONE);
            holder.tvStreakCount.setVisibility(View.GONE);
        }

        // Set reminder time
        if (habit.isReminderEnabled() && habit.getReminderTime() != null) {
            holder.layoutReminder.setVisibility(View.VISIBLE);
            holder.tvReminderTime.setText(formatReminderTime(habit.getReminderTime()));
        } else {
            holder.layoutReminder.setVisibility(View.GONE);
        }
        
        // Set location
        if (habit.hasLocation() && habit.getLocationName() != null) {
            holder.layoutLocation.setVisibility(View.VISIBLE);
            holder.tvLocationName.setText(habit.getLocationName());
        } else {
            holder.layoutLocation.setVisibility(View.GONE);
        }

        // Set completion checkbox
        holder.checkboxComplete.setOnCheckedChangeListener(null);
        holder.checkboxComplete.setChecked(isCompleted);
        holder.checkboxComplete.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                completedToday.add(habit.getId());
                animateCompletion(holder.cardView);
            } else {
                completedToday.remove(habit.getId());
            }
            if (listener != null) {
                listener.onCompletionToggled(habit, holder.getAdapterPosition(), checked);
            }
        });


        // Card click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHabitClick(habit, holder.getAdapterPosition());
            }
        });

        // Long press listener
        holder.cardView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onHabitLongClick(habit, holder.getAdapterPosition(), v);
            }
            return true;
        });

        // Visual feedback for completed habits
        if (isCompleted) {
            holder.cardView.setAlpha(0.7f);
            holder.tvHabitName.setAlpha(0.7f);
        } else {
            holder.cardView.setAlpha(1.0f);
            holder.tvHabitName.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return habitList != null ? habitList.size() : 0;
    }

    private void setPriorityIndicator(View indicator, String priority) {
        int color;
        Priority p = Priority.fromString(priority);
        switch (p) {
            case HIGH:
                color = ContextCompat.getColor(context, R.color.priority_high);
                break;
            case LOW:
                color = ContextCompat.getColor(context, R.color.priority_low);
                break;
            case MEDIUM:
            default:
                color = ContextCompat.getColor(context, R.color.priority_medium);
                break;
        }
        indicator.setBackgroundColor(color);
    }

    private void setCategoryLabel(TextView tvCategory, String category) {
        if (category == null || category.isEmpty()) {
            category = "Other";
        }
        tvCategory.setText(category);

        int color;
        switch (category.toLowerCase()) {
            case "health":
                color = ContextCompat.getColor(context, R.color.category_health);
                break;
            case "work":
                color = ContextCompat.getColor(context, R.color.category_work);
                break;
            case "personal":
                color = ContextCompat.getColor(context, R.color.category_personal);
                break;
            case "learning":
                color = ContextCompat.getColor(context, R.color.category_learning);
                break;
            default:
                color = ContextCompat.getColor(context, R.color.category_other);
                break;
        }

        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(16f);
        tvCategory.setBackground(background);
    }


    private String formatReminderTime(String time24h) {
        if (time24h == null || time24h.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat input = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date date = input.parse(time24h);
            return date != null ? output.format(date) : time24h;
        } catch (ParseException e) {
            return time24h;
        }
    }

    private void animateCompletion(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f, 1f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(200);
        animatorSet.start();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        View viewPriorityIndicator;
        TextView tvHabitName;
        TextView tvCategory;
        TextView tvStreakIcon;
        TextView tvStreakCount;
        LinearLayout layoutReminder;
        TextView tvReminderTime;
        LinearLayout layoutLocation;
        TextView tvLocationName;
        CheckBox checkboxComplete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardHabit);
            viewPriorityIndicator = itemView.findViewById(R.id.viewPriorityIndicator);
            tvHabitName = itemView.findViewById(R.id.tvHabitName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvStreakIcon = itemView.findViewById(R.id.tvStreakIcon);
            tvStreakCount = itemView.findViewById(R.id.tvStreakCount);
            layoutReminder = itemView.findViewById(R.id.layoutReminder);
            tvReminderTime = itemView.findViewById(R.id.tvReminderTime);
            layoutLocation = itemView.findViewById(R.id.layoutLocation);
            tvLocationName = itemView.findViewById(R.id.tvLocationName);
            checkboxComplete = itemView.findViewById(R.id.checkboxComplete);
        }
    }
}
