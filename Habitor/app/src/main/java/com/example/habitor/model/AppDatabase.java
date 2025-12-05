package com.example.habitor.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Habit.class, HabitHistory.class, Category.class, SyncOperation.class}, version = 5)
public abstract class AppDatabase extends RoomDatabase {

    public abstract HabitDao habitDao();

    // MIGRATION from version 1 → 2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Habit ADD COLUMN note TEXT DEFAULT ''");
        }
    };

    // MIGRATION from version 2 → 3
    // Adds new columns to Habit table and creates Category and SyncQueue tables
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add new columns to Habit table with default values
            db.execSQL("ALTER TABLE Habit ADD COLUMN firebaseId TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE Habit ADD COLUMN lastSyncedAt INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE Habit ADD COLUMN streakCount INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE Habit ADD COLUMN reminderTime TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE Habit ADD COLUMN isReminderEnabled INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE Habit ADD COLUMN repeatPattern TEXT DEFAULT 'DAILY'");
            db.execSQL("ALTER TABLE Habit ADD COLUMN repeatDays TEXT DEFAULT '[]'");
            db.execSQL("ALTER TABLE Habit ADD COLUMN customIntervalDays INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE Habit ADD COLUMN priority TEXT DEFAULT 'MEDIUM'");
            db.execSQL("ALTER TABLE Habit ADD COLUMN category TEXT DEFAULT 'Other'");

            // Create Category table
            db.execSQL("CREATE TABLE IF NOT EXISTS Category (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT, " +
                    "color TEXT, " +
                    "isDefault INTEGER NOT NULL DEFAULT 0)");

            // Insert default categories
            db.execSQL("INSERT INTO Category (name, color, isDefault) VALUES ('Health', '#4CAF50', 1)");
            db.execSQL("INSERT INTO Category (name, color, isDefault) VALUES ('Work', '#2196F3', 1)");
            db.execSQL("INSERT INTO Category (name, color, isDefault) VALUES ('Personal', '#9C27B0', 1)");
            db.execSQL("INSERT INTO Category (name, color, isDefault) VALUES ('Learning', '#FF9800', 1)");
            db.execSQL("INSERT INTO Category (name, color, isDefault) VALUES ('Other', '#607D8B', 1)");

            // Create SyncQueue table
            db.execSQL("CREATE TABLE IF NOT EXISTS SyncQueue (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "operationType TEXT, " +
                    "habitId INTEGER NOT NULL DEFAULT 0, " +
                    "habitJson TEXT, " +
                    "createdAt INTEGER NOT NULL DEFAULT 0)");
        }
    };

    // MIGRATION from version 3 → 4
    // Adds location fields to Habit table
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add location columns to Habit table
            db.execSQL("ALTER TABLE Habit ADD COLUMN locationName TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE Habit ADD COLUMN latitude REAL DEFAULT NULL");
            db.execSQL("ALTER TABLE Habit ADD COLUMN longitude REAL DEFAULT NULL");
            db.execSQL("ALTER TABLE Habit ADD COLUMN locationRadius INTEGER NOT NULL DEFAULT 100");
            db.execSQL("ALTER TABLE Habit ADD COLUMN isLocationReminderEnabled INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE Habit ADD COLUMN locationTriggerType TEXT DEFAULT 'ENTER'");
        }
    };

    // MIGRATION from version 4 → 5
    // Adds imagePath field to Habit table for habit image attachment
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add imagePath column to Habit table
            db.execSQL("ALTER TABLE Habit ADD COLUMN imagePath TEXT DEFAULT NULL");
        }
    };

    // Singleton to avoid creating multiple DB instances
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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
