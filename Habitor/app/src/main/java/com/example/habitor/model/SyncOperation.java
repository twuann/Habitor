package com.example.habitor.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity for queuing offline sync operations.
 * When the device is offline, changes are stored here and processed when connectivity is restored.
 */
@Entity(tableName = "SyncQueue")
public class SyncOperation {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String operationType;    // INSERT, UPDATE, DELETE
    public int habitId;
    public String habitJson;        // Serialized habit data
    public long createdAt;

    // Default constructor for Room
    public SyncOperation() {
        this.operationType = "";
        this.habitId = 0;
        this.habitJson = "";
        this.createdAt = System.currentTimeMillis();
    }

    @Ignore
    public SyncOperation(String operationType, int habitId, String habitJson) {
        this.operationType = operationType;
        this.habitId = habitId;
        this.habitJson = habitJson;
        this.createdAt = System.currentTimeMillis();
    }

    // ===========================
    // OPERATION TYPE CONSTANTS
    // ===========================

    @Ignore
    public static final String OPERATION_INSERT = "INSERT";
    @Ignore
    public static final String OPERATION_UPDATE = "UPDATE";
    @Ignore
    public static final String OPERATION_DELETE = "DELETE";

    // ===========================
    // GETTERS AND SETTERS
    // ===========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public int getHabitId() {
        return habitId;
    }

    public void setHabitId(int habitId) {
        this.habitId = habitId;
    }

    public String getHabitJson() {
        return habitJson;
    }

    public void setHabitJson(String habitJson) {
        this.habitJson = habitJson;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncOperation that = (SyncOperation) o;
        return id == that.id &&
                habitId == that.habitId &&
                createdAt == that.createdAt &&
                objectsEquals(operationType, that.operationType) &&
                objectsEquals(habitJson, that.habitJson);
    }

    private static boolean objectsEquals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (operationType != null ? operationType.hashCode() : 0);
        result = 31 * result + habitId;
        return result;
    }
}
