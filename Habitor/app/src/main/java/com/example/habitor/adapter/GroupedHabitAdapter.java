package com.example.habitor.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
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
import com.example.habitor.model.Category;
import com.example.habitor.model.Habit;
import com.example.habitor.model.Priority;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Adapter for displaying habits grouped by category with collapsible sections.
 * Requirements: 10.2, 10.5
 * - Group habits by category with collapsible sections
 * - Show category headers with completion stats
 */
public class GroupedHabitAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_HABIT = 1;

    private final Context context;
    private List<Object> displayItems; // Mixed list of CategoryHeader and Habit
    private final Set<Integer> completedToday;
    private final Set<String> collapsedCategories;
    private OnHabitInteractionListener listener;
    private Map<String, String> categoryColors;


    /**
     * Represents a category header in the grouped list.
     */
    public static class CategoryHeader {
        public String categoryName;
        public String color;
        public int totalHabits;
        public int completedHabits;
        public boolean isExpanded;

        public CategoryHeader(String categoryName, String color) {
            this.categoryName = categoryName;
            this.color = color;
            this.totalHabits = 0;
            this.completedHabits = 0;
            this.isExpanded = true;
        }
    }

    public interface OnHabitInteractionListener {
        void onHabitClick(Habit habit, int position);
        void onHabitLongClick(Habit habit, int position, View anchorView);
        void onCompletionToggled(Habit habit, int position, boolean isCompleted);
    }

    public GroupedHabitAdapter(Context context) {
        this.context = context;
        this.displayItems = new ArrayList<>();
        this.completedToday = new HashSet<>();
        this.collapsedCategories = new HashSet<>();
        this.categoryColors = new HashMap<>();
        initDefaultCategoryColors();
    }

    private void initDefaultCategoryColors() {
        categoryColors.put("Health", "#4CAF50");
        categoryColors.put("Work", "#2196F3");
        categoryColors.put("Personal", "#9C27B0");
        categoryColors.put("Learning", "#FF9800");
        categoryColors.put("Other", "#607D8B");
    }

    public void setCategoryColors(List<Category> categories) {
        for (Category category : categories) {
            categoryColors.put(category.getName(), category.getColor());
        }
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

    /**
     * Update the habit list and rebuild the grouped display.
     * Groups habits by category and creates headers with completion stats.
     */
    public void updateHabitList(List<Habit> habits) {
        // Group habits by category
        Map<String, List<Habit>> groupedHabits = new LinkedHashMap<>();
        
        // Define category order
        String[] categoryOrder = {"Health", "Work", "Personal", "Learning", "Other"};
        for (String cat : categoryOrder) {
            groupedHabits.put(cat, new ArrayList<>());
        }

        // Add habits to their categories
        for (Habit habit : habits) {
            String category = habit.getCategory();
            if (category == null || category.isEmpty()) {
                category = "Other";
            }
            if (!groupedHabits.containsKey(category)) {
                groupedHabits.put(category, new ArrayList<>());
            }
            groupedHabits.get(category).add(habit);
        }

        // Build display items list
        displayItems.clear();
        for (Map.Entry<String, List<Habit>> entry : groupedHabits.entrySet()) {
            List<Habit> categoryHabits = entry.getValue();
            if (categoryHabits.isEmpty()) {
                continue; // Skip empty categories
            }

            String categoryName = entry.getKey();
            String color = categoryColors.getOrDefault(categoryName, "#607D8B");
            
            // Create header
            CategoryHeader header = new CategoryHeader(categoryName, color);
            header.totalHabits = categoryHabits.size();
            header.completedHabits = countCompletedInCategory(categoryHabits);
            header.isExpanded = !collapsedCategories.contains(categoryName);
            displayItems.add(header);

            // Add habits if expanded
            if (header.isExpanded) {
                displayItems.addAll(categoryHabits);
            }
        }

        notifyDataSetChanged();
    }

    private int countCompletedInCategory(List<Habit> habits) {
        int count = 0;
        for (Habit habit : habits) {
            if (completedToday.contains(habit.getId())) {
                count++;
            }
        }
        return count;
    }


    public int getCompletedCount() {
        return completedToday.size();
    }

    public int getTotalHabitCount() {
        int count = 0;
        for (Object item : displayItems) {
            if (item instanceof Habit) {
                count++;
            }
        }
        // Also count collapsed habits
        for (Object item : displayItems) {
            if (item instanceof CategoryHeader) {
                CategoryHeader header = (CategoryHeader) item;
                if (!header.isExpanded) {
                    count += header.totalHabits;
                }
            }
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position) instanceof CategoryHeader ? VIEW_TYPE_HEADER : VIEW_TYPE_HABIT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_category_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_habit_card, parent, false);
            return new HabitViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            bindHeaderViewHolder((HeaderViewHolder) holder, (CategoryHeader) displayItems.get(position));
        } else if (holder instanceof HabitViewHolder) {
            bindHabitViewHolder((HabitViewHolder) holder, (Habit) displayItems.get(position), position);
        }
    }

    private void bindHeaderViewHolder(HeaderViewHolder holder, CategoryHeader header) {
        holder.tvCategoryName.setText(header.categoryName);
        
        // Set completion stats
        String stats = header.completedHabits + "/" + header.totalHabits + " completed";
        holder.tvCompletionStats.setText(stats);

        // Set category color indicator
        try {
            GradientDrawable colorIndicator = new GradientDrawable();
            colorIndicator.setShape(GradientDrawable.OVAL);
            colorIndicator.setColor(Color.parseColor(header.color));
            holder.viewCategoryColor.setBackground(colorIndicator);
        } catch (Exception e) {
            holder.viewCategoryColor.setBackgroundColor(Color.GRAY);
        }

        // Set expand/collapse arrow
        holder.tvExpandArrow.setText(header.isExpanded ? "▼" : "▶");

        // Click to toggle expand/collapse
        holder.itemView.setOnClickListener(v -> {
            if (header.isExpanded) {
                collapsedCategories.add(header.categoryName);
            } else {
                collapsedCategories.remove(header.categoryName);
            }
            // Rebuild the list
            rebuildDisplayList();
        });
    }

    private void rebuildDisplayList() {
        // Get all habits from current display
        List<Habit> allHabits = new ArrayList<>();
        for (Object item : displayItems) {
            if (item instanceof Habit) {
                allHabits.add((Habit) item);
            }
        }
        
        // Also need to get habits from collapsed sections
        // This requires keeping track of all habits separately
        // For now, trigger a full refresh through listener
        notifyDataSetChanged();
    }


    private void bindHabitViewHolder(HabitViewHolder holder, Habit habit, int position) {
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
            // Update header stats
            updateHeaderStats();
            if (listener != null) {
                listener.onCompletionToggled(habit, position, checked);
            }
        });

        // Card click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHabitClick(habit, position);
            }
        });

        // Long press listener
        holder.cardView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onHabitLongClick(habit, position, v);
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

    private void updateHeaderStats() {
        // Update completion counts in headers
        for (int i = 0; i < displayItems.size(); i++) {
            Object item = displayItems.get(i);
            if (item instanceof CategoryHeader) {
                CategoryHeader header = (CategoryHeader) item;
                int completed = 0;
                // Count completed habits in this category
                for (int j = i + 1; j < displayItems.size(); j++) {
                    Object nextItem = displayItems.get(j);
                    if (nextItem instanceof CategoryHeader) {
                        break;
                    }
                    if (nextItem instanceof Habit) {
                        Habit habit = (Habit) nextItem;
                        if (completedToday.contains(habit.getId())) {
                            completed++;
                        }
                    }
                }
                header.completedHabits = completed;
                notifyItemChanged(i);
            }
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
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

        String colorHex = categoryColors.getOrDefault(category, "#607D8B");
        try {
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.parseColor(colorHex));
            background.setCornerRadius(16f);
            tvCategory.setBackground(background);
        } catch (Exception e) {
            tvCategory.setBackgroundColor(Color.GRAY);
        }
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

    // ===========================
    // VIEW HOLDERS
    // ===========================

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        View viewCategoryColor;
        TextView tvCategoryName;
        TextView tvCompletionStats;
        TextView tvExpandArrow;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCategoryColor = itemView.findViewById(R.id.viewCategoryColor);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvCompletionStats = itemView.findViewById(R.id.tvCompletionStats);
            tvExpandArrow = itemView.findViewById(R.id.tvExpandArrow);
        }
    }

    public static class HabitViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        View viewPriorityIndicator;
        TextView tvHabitName;
        TextView tvCategory;
        TextView tvStreakIcon;
        TextView tvStreakCount;
        LinearLayout layoutReminder;
        TextView tvReminderTime;
        CheckBox checkboxComplete;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardHabit);
            viewPriorityIndicator = itemView.findViewById(R.id.viewPriorityIndicator);
            tvHabitName = itemView.findViewById(R.id.tvHabitName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvStreakIcon = itemView.findViewById(R.id.tvStreakIcon);
            tvStreakCount = itemView.findViewById(R.id.tvStreakCount);
            layoutReminder = itemView.findViewById(R.id.layoutReminder);
            tvReminderTime = itemView.findViewById(R.id.tvReminderTime);
            checkboxComplete = itemView.findViewById(R.id.checkboxComplete);
        }
    }
}
