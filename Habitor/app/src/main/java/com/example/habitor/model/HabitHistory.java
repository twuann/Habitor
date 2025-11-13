package com.example.habitor.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class HabitHistory {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String habitName;
    public String date; // yyyy-MM-dd
}
