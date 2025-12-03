package com.example.habitor.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface HabitDao {
    @Query("SELECT * FROM Habit WHERE isDeleted = 0")
    List<Habit> getAll();

    @Query("SELECT * FROM Habit WHERE isDeleted = 1")
    List<Habit> getTrash();

    @Query("SELECT * FROM Habit WHERE id = :habitId")
    Habit getHabitById(int habitId);

    @Insert
    long insert(Habit habit);

    @Update
    void update(Habit habit);

    @Query("UPDATE Habit SET isDeleted = 1 WHERE id = :habitId")
    void moveToTrash(int habitId);

    @Query("UPDATE Habit SET isDeleted = 0 WHERE id = :habitId")
    void restoreHabit(int habitId);

    @Delete
    void deleteHabit(Habit habit);

    // ====== Priority and Category Filtering ======
    @Query("SELECT * FROM Habit WHERE isDeleted = 0 AND priority = :priority")
    List<Habit> getHabitsByPriority(String priority);

    @Query("SELECT * FROM Habit WHERE isDeleted = 0 AND category = :category")
    List<Habit> getHabitsByCategory(String category);

    @Query("SELECT * FROM Habit WHERE isDeleted = 0 ORDER BY " +
            "CASE priority " +
            "WHEN 'HIGH' THEN 1 " +
            "WHEN 'MEDIUM' THEN 2 " +
            "WHEN 'LOW' THEN 3 " +
            "ELSE 4 END")
    List<Habit> getAllHabitsSortedByPriority();

    // ====== Reminder Management ======
    @Query("SELECT * FROM Habit WHERE isDeleted = 0 AND isReminderEnabled = 1")
    List<Habit> getHabitsWithReminders();

    @Query("UPDATE Habit SET reminderTime = :reminderTime, isReminderEnabled = :isEnabled, " +
            "repeatPattern = :repeatPattern, repeatDays = :repeatDays, customIntervalDays = :customIntervalDays " +
            "WHERE id = :habitId")
    void updateReminderSettings(int habitId, String reminderTime, boolean isEnabled, 
                                String repeatPattern, String repeatDays, int customIntervalDays);

    // ====== Sync Operations ======
    @Query("SELECT * FROM Habit WHERE lastSyncedAt = 0 OR firebaseId IS NULL")
    List<Habit> getUnsyncedHabits();

    @Query("UPDATE Habit SET firebaseId = :firebaseId, lastSyncedAt = :syncTime WHERE id = :habitId")
    void updateSyncStatus(int habitId, String firebaseId, long syncTime);

    // ====== History ======
    @Insert
    void insertHistory(HabitHistory history);

    @Query("SELECT * FROM HabitHistory WHERE habitName = :habitName")
    List<HabitHistory> getHistoryForHabit(String habitName);

    @Query("SELECT DISTINCT date FROM HabitHistory")
    List<String> getAllDates();

    // Update note for Habit
    @Query("UPDATE Habit SET note = :note WHERE id = :habitId")
    void updateNote(int habitId, String note);

    // ====== SyncQueue Operations ======
    @Insert
    void insertSyncOperation(SyncOperation operation);

    @Query("SELECT * FROM SyncQueue ORDER BY createdAt ASC")
    List<SyncOperation> getAllSyncOperations();

    @Delete
    void deleteSyncOperation(SyncOperation operation);

    @Query("DELETE FROM SyncQueue")
    void clearSyncQueue();

    // ====== Category Operations ======
    @Insert
    long insertCategory(Category category);

    @Update
    void updateCategory(Category category);

    @Delete
    void deleteCategory(Category category);

    @Query("SELECT * FROM Category ORDER BY isDefault DESC, name ASC")
    List<Category> getAllCategories();

    @Query("SELECT * FROM Category WHERE id = :categoryId")
    Category getCategoryById(int categoryId);

    @Query("SELECT * FROM Category WHERE name = :name LIMIT 1")
    Category getCategoryByName(String name);

    @Query("SELECT * FROM Category WHERE isDefault = 1")
    List<Category> getDefaultCategories();

    @Query("SELECT * FROM Category WHERE isDefault = 0")
    List<Category> getCustomCategories();

    @Query("SELECT COUNT(*) FROM Category")
    int getCategoryCount();

    // ====== End of Day Reminder ======
    @Query("SELECT * FROM Habit WHERE isDeleted = 0 AND priority = 'HIGH'")
    List<Habit> getHighPriorityHabits();

    @Query("SELECT * FROM HabitHistory WHERE date = :date")
    List<HabitHistory> getHistoryForDate(String date);
}

