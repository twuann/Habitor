package com.example.habitor.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Habit")
public class Habit {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public boolean isDeleted;

    public String note;  // 🔥 thêm trường NOTE

    // Constructor mặc định khi thêm Habit mới
    public Habit(String name) {
        this.name = name;
        this.isDeleted = false;
        this.note = "";
    }

    // ===========================
    // 🔥 GETTER - SETTER BẮT BUỘC
    // ===========================

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
