package com.example.habitor.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface HabitDao {
    @Query("SELECT * FROM Habit WHERE isDeleted = 0")
    List<Habit> getAll();

    @Query("SELECT * FROM Habit WHERE isDeleted = 1")
    List<Habit> getTrash();

    @Insert
    void insert(Habit habit);

    @Query("UPDATE Habit SET isDeleted = 1 WHERE id = :habitId")
    void moveToTrash(int habitId);

    @Query("UPDATE Habit SET isDeleted = 0 WHERE id = :habitId")
    void restoreHabit(int habitId);

    @Delete
    void deleteHabit(Habit habit);

    // ====== History ======
    @Insert
    void insertHistory(HabitHistory history);

    @Query("SELECT * FROM HabitHistory WHERE habitName = :habitName")
    List<HabitHistory> getHistoryForHabit(String habitName);

    @Query("SELECT DISTINCT date FROM HabitHistory")
    List<String> getAllDates();

    // 🔥 Update ghi chú (note) cho Habit
    @Query("UPDATE Habit SET note = :note WHERE id = :habitId")
    void updateNote(int habitId, String note);

}

