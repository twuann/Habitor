package com.example.habitor.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habitor.R;
import com.example.habitor.adapter.GroupedHabitAdapter;
import com.example.habitor.adapter.HabitCardAdapter;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Category;
import com.example.habitor.model.Habit;
import com.example.habitor.model.HabitDao;
import com.example.habitor.model.HabitHistory;
import com.example.habitor.model.Priority;
import com.example.habitor.repository.CategoryRepository;
import com.example.habitor.repository.HabitRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Enhanced HomeFragment with card-based layout and progress tracking.
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 9.4, 10.2, 10.4, 10.5
 */
public class HomeFragment extends Fragment implements 
        HabitCardAdapter.OnHabitInteractionListener,
        GroupedHabitAdapter.OnHabitInteractionListener {

    private RecyclerView recyclerView;
    private HabitCardAdapter adapter;
    private GroupedHabitAdapter groupedAdapter;
    private List<Habit> habitList;
    private HabitDao habitDao;
    private CategoryRepository categoryRepository;

    // Progress UI
    private TextView tvProgressCount;
    private ProgressBar progressBar;
    private LinearLayout layoutCelebration;
    private LinearLayout layoutFilterChips;

    // Filter state
    private String currentPriorityFilter = null;
    private String currentCategoryFilter = null;
    private boolean sortByPriority = false;
    private boolean groupByCategory = false;
    private TextView selectedChip = null;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerHabits);
        tvProgressCount = view.findViewById(R.id.tvProgressCount);
        progressBar = view.findViewById(R.id.progressBar);
        layoutCelebration = view.findViewById(R.id.layoutCelebration);
        layoutFilterChips = view.findViewById(R.id.layoutFilterChips);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize database and repositories
        AppDatabase db = AppDatabase.getInstance(requireContext());
        habitDao = db.habitDao();
        categoryRepository = new CategoryRepository(requireContext());

        // Setup filter chips
        setupFilterChips();

        // Load habits
        loadHabits();

        return view;
    }

    private void setupFilterChips() {
        layoutFilterChips.removeAllViews();

        // Add "All" chip
        addFilterChip("All", null, null, true);

        // Add priority filter chips
        addFilterChip("🔴 High", "HIGH", null, false);
        addFilterChip("🟡 Medium", "MEDIUM", null, false);
        addFilterChip("🟢 Low", "LOW", null, false);

        // Add separator
        View separator = new View(getContext());
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(1, 
            (int) (24 * getResources().getDisplayMetrics().density));
        sepParams.setMargins(8, 0, 8, 0);
        separator.setLayoutParams(sepParams);
        separator.setBackgroundColor(getResources().getColor(R.color.progress_background, null));
        layoutFilterChips.addView(separator);

        // Add category filter chips
        addFilterChip("Health", null, "Health", false);
        addFilterChip("Work", null, "Work", false);
        addFilterChip("Personal", null, "Personal", false);
        addFilterChip("Learning", null, "Learning", false);

        // Add sort by priority chip
        View separator2 = new View(getContext());
        separator2.setLayoutParams(sepParams);
        separator2.setBackgroundColor(getResources().getColor(R.color.progress_background, null));
        layoutFilterChips.addView(separator2);

        addSortChip("Sort by Priority");
        
        // Add group by category chip
        addGroupByCategoryChip("Group by Category");
    }

    private void addFilterChip(String label, String priorityFilter, String categoryFilter, boolean isDefault) {
        TextView chip = createChip(label);
        
        if (isDefault) {
            selectChip(chip);
        }

        chip.setOnClickListener(v -> {
            selectChip(chip);
            currentPriorityFilter = priorityFilter;
            currentCategoryFilter = categoryFilter;
            sortByPriority = false;
            loadHabits();
        });

        layoutFilterChips.addView(chip);
    }

    private void addSortChip(String label) {
        TextView chip = createChip(label);

        chip.setOnClickListener(v -> {
            if (sortByPriority) {
                sortByPriority = false;
                chip.setSelected(false);
                chip.setTextColor(getResources().getColor(R.color.text_primary, null));
            } else {
                sortByPriority = true;
                groupByCategory = false; // Disable grouping when sorting
                chip.setSelected(true);
                chip.setTextColor(getResources().getColor(R.color.white, null));
            }
            loadHabits();
        });

        layoutFilterChips.addView(chip);
    }

    private void addGroupByCategoryChip(String label) {
        TextView chip = createChip(label);

        chip.setOnClickListener(v -> {
            if (groupByCategory) {
                groupByCategory = false;
                chip.setSelected(false);
                chip.setTextColor(getResources().getColor(R.color.text_primary, null));
            } else {
                groupByCategory = true;
                sortByPriority = false; // Disable sorting when grouping
                currentPriorityFilter = null;
                currentCategoryFilter = null;
                chip.setSelected(true);
                chip.setTextColor(getResources().getColor(R.color.white, null));
            }
            loadHabits();
        });

        layoutFilterChips.addView(chip);
    }

    private TextView createChip(String label) {
        TextView chip = new TextView(getContext());
        chip.setText(label);
        chip.setTextSize(12);
        chip.setPadding(
            (int) (12 * getResources().getDisplayMetrics().density),
            (int) (6 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density),
            (int) (6 * getResources().getDisplayMetrics().density)
        );
        chip.setBackgroundResource(R.drawable.bg_filter_chip);
        chip.setTextColor(getResources().getColor(R.color.text_primary, null));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 0, 4, 0);
        chip.setLayoutParams(params);

        return chip;
    }

    private void selectChip(TextView chip) {
        // Deselect previous chip
        if (selectedChip != null) {
            selectedChip.setSelected(false);
            selectedChip.setTextColor(getResources().getColor(R.color.text_primary, null));
        }

        // Select new chip
        chip.setSelected(true);
        chip.setTextColor(getResources().getColor(R.color.white, null));
        selectedChip = chip;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHabits();
    }

    private void loadHabits() {
        // Load habits based on current filter/sort settings
        if (sortByPriority) {
            habitList = new ArrayList<>(habitDao.getAllHabitsSortedByPriority());
        } else if (currentPriorityFilter != null) {
            habitList = new ArrayList<>(habitDao.getHabitsByPriority(currentPriorityFilter));
        } else if (currentCategoryFilter != null) {
            habitList = new ArrayList<>(habitDao.getHabitsByCategory(currentCategoryFilter));
        } else {
            habitList = new ArrayList<>(habitDao.getAll());
        }



        // Get today's completed habits
        Set<Integer> completedToday = getCompletedHabitsToday();

        // Use grouped adapter or flat adapter based on setting
        if (groupByCategory) {
            loadGroupedHabits(completedToday);
        } else {
            loadFlatHabits(completedToday);
        }

        // Update progress
        updateProgress(completedToday.size(), habitList.size());
    }

    /**
     * Load habits in grouped view by category.
     * Requirements: 10.2, 10.5
     */
    private void loadGroupedHabits(Set<Integer> completedToday) {
        if (groupedAdapter == null) {
            groupedAdapter = new GroupedHabitAdapter(requireContext());
            groupedAdapter.setOnHabitInteractionListener(this);
            
            // Set category colors from repository
            List<Category> categories = categoryRepository.getAllCategories();
            groupedAdapter.setCategoryColors(categories);
        }
        
        groupedAdapter.setCompletedHabits(completedToday);
        groupedAdapter.updateHabitList(habitList);
        
        if (recyclerView.getAdapter() != groupedAdapter) {
            recyclerView.setAdapter(groupedAdapter);
        }
    }

    /**
     * Load habits in flat (non-grouped) view.
     */
    private void loadFlatHabits(Set<Integer> completedToday) {
        if (adapter == null) {
            adapter = new HabitCardAdapter(requireContext(), habitList);
            adapter.setOnHabitInteractionListener(this);
        }
        
        adapter.updateHabitList(habitList);
        adapter.setCompletedHabits(completedToday);
        
        if (recyclerView.getAdapter() != adapter) {
            recyclerView.setAdapter(adapter);
        }
    }

    private Set<Integer> getCompletedHabitsToday() {
        Set<Integer> completed = new HashSet<>();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        for (Habit habit : habitList) {
            List<HabitHistory> history = habitDao.getHistoryForHabit(habit.getName());
            for (HabitHistory h : history) {
                if (today.equals(h.date)) {
                    completed.add(habit.getId());
                    break;
                }
            }
        }
        return completed;
    }

    private void updateProgress(int completed, int total) {
        if (total == 0) {
            tvProgressCount.setText("0/0");
            progressBar.setProgress(0);
            layoutCelebration.setVisibility(View.GONE);
            return;
        }

        tvProgressCount.setText(completed + "/" + total);
        int percentage = (int) ((completed * 100.0f) / total);
        progressBar.setProgress(percentage);

        // Check for celebration (Requirement 7.5)
        if (completed == total && total > 0) {
            showCelebration();
        } else {
            layoutCelebration.setVisibility(View.GONE);
        }
    }

    private void showCelebration() {
        if (layoutCelebration.getVisibility() == View.VISIBLE) {
            return; // Already showing
        }
        
        layoutCelebration.setVisibility(View.VISIBLE);
        
        // Animate the celebration banner with bounce effect
        Animation bounceAnim = AnimationUtils.loadAnimation(getContext(), R.anim.celebration_bounce);
        layoutCelebration.startAnimation(bounceAnim);
        
        // Show a toast as well
        Toast.makeText(getContext(), "🎉 All habits completed! Great job! 🎉", Toast.LENGTH_LONG).show();
    }

    // ===========================
    // HabitCardAdapter.OnHabitInteractionListener
    // ===========================

    @Override
    public void onHabitClick(Habit habit, int position) {
        // Navigate to habit detail screen (Requirement 8.1)
        navigateToHabitDetail(habit);
    }

    private void navigateToHabitDetail(Habit habit) {
        HabitDetailFragment detailFragment = HabitDetailFragment.newInstance(habit.getId());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onHabitLongClick(Habit habit, int position, View anchorView) {
        // Show context menu (Requirement 7.4)
        showContextMenu(habit, position, anchorView);
    }

    @Override
    public void onCompletionToggled(Habit habit, int position, boolean isCompleted) {
        // Record completion in history
        if (isCompleted) {
            recordCompletion(habit);
        } else {
            // Remove today's completion
            removeCompletion(habit);
        }
        
        // Update progress based on current adapter
        if (groupByCategory && groupedAdapter != null) {
            updateProgress(groupedAdapter.getCompletedCount(), habitList.size());
        } else if (adapter != null) {
            updateProgress(adapter.getCompletedCount(), adapter.getTotalCount());
        }
    }

    private void toggleCompletion(Habit habit) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        List<HabitHistory> history = habitDao.getHistoryForHabit(habit.getName());
        
        boolean completedToday = false;
        for (HabitHistory h : history) {
            if (today.equals(h.date)) {
                completedToday = true;
                break;
            }
        }

        if (completedToday) {
            removeCompletion(habit);
            Toast.makeText(getContext(), "Marked as incomplete", Toast.LENGTH_SHORT).show();
        } else {
            recordCompletion(habit);
            Toast.makeText(getContext(), "Completed! 🎉", Toast.LENGTH_SHORT).show();
        }

        // Refresh the list
        loadHabits();
    }

    private void recordCompletion(Habit habit) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // Check if already completed today
        List<HabitHistory> history = habitDao.getHistoryForHabit(habit.getName());
        for (HabitHistory h : history) {
            if (today.equals(h.date)) {
                return; // Already completed
            }
        }

        // Add completion record
        HabitHistory completion = new HabitHistory();
        completion.habitName = habit.getName();
        completion.date = today;
        habitDao.insertHistory(completion);

        // Update streak count
        habit.setStreakCount(habit.getStreakCount() + 1);
        habitDao.update(habit);
    }

    private void removeCompletion(Habit habit) {
        // Note: This would require a delete query in HabitDao
        // For now, we'll just update the streak
        if (habit.getStreakCount() > 0) {
            habit.setStreakCount(habit.getStreakCount() - 1);
            habitDao.update(habit);
        }
    }


    private void showContextMenu(Habit habit, int position, View anchorView) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.menu_habit_context, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit) {
                editHabit(habit);
                return true;
            } else if (itemId == R.id.action_delete) {
                deleteHabit(habit, position);
                return true;
            } else if (itemId == R.id.action_share) {
                shareHabit(habit);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void editHabit(Habit habit) {
        AddEditHabitBottomSheet bottomSheet = AddEditHabitBottomSheet.newInstance(habit.getId());
        bottomSheet.setOnHabitSavedListener(savedHabit -> {
            loadHabits();
        });
        bottomSheet.show(getParentFragmentManager(), "EditHabitBottomSheet");
    }

    /**
     * Public method to refresh habits list.
     * Called from MainActivity after adding a new habit.
     */
    public void refreshHabits() {
        loadHabits();
    }

    private void deleteHabit(Habit habit, int position) {
        habitDao.moveToTrash(habit.getId());
        habitList.remove(position);
        adapter.notifyItemRemoved(position);
        Toast.makeText(getContext(), "Moved to Trash", Toast.LENGTH_SHORT).show();
        
        // Update progress
        updateProgress(adapter.getCompletedCount(), adapter.getTotalCount());
    }

    private void shareHabit(Habit habit) {
        String shareText = "I'm tracking my habit: " + habit.getName();
        if (habit.getStreakCount() > 0) {
            shareText += "\n🔥 Current streak: " + habit.getStreakCount() + " days!";
        }
        shareText += "\n\nTracked with Habitor app";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Habit"));
    }

    // ===========================
    // Filter and Sort Methods (for task 10.5)
    // ===========================

    public void filterByPriority(String priority) {
        this.currentPriorityFilter = priority;
        this.currentCategoryFilter = null;
        loadHabits();
    }

    public void filterByCategory(String category) {
        this.currentCategoryFilter = category;
        this.currentPriorityFilter = null;
        loadHabits();
    }

    public void clearFilters() {
        this.currentPriorityFilter = null;
        this.currentCategoryFilter = null;
        this.sortByPriority = false;
        loadHabits();
    }

    public void setSortByPriority(boolean sortByPriority) {
        this.sortByPriority = sortByPriority;
        loadHabits();
    }
}
