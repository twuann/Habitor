package com.example.habitor.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Habit.class, HabitHistory.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {

    public abstract HabitDao habitDao();

    // 🔥 MIGRATION từ version 1 → 2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Habit ADD COLUMN note TEXT DEFAULT ''");
        }
    };

    // Singleton tránh tạo nhiều DB instance
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "habitor_db"
                            )
                            .addMigrations(MIGRATION_1_2)  // 🔥 thêm migration
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
