package com.example.habitor.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "Habit")
public class Habit {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public boolean isDeleted;
    public String note;

    // Firebase sync fields
    public String firebaseId;
    public long lastSyncedAt;
    public int streakCount;

    // Reminder fields
    public String reminderTime;         // Format: "HH:mm"
    public boolean isReminderEnabled;
    public String repeatPattern;        // DAILY, WEEKLY, CUSTOM
    public String repeatDays;           // JSON array: [1,3,5] for Mon,Wed,Fri
    public int customIntervalDays;      // For CUSTOM pattern

    // Organization fields
    public String priority;             // HIGH, MEDIUM, LOW
    public String category;             // Health, Work, Personal, Learning, Other

    // Location fields
    public String locationName;         // Name of the location (e.g., "Gym ABC")
    public Double latitude;             // Latitude coordinate
    public Double longitude;            // Longitude coordinate
    public int locationRadius;          // Geofence radius in meters (50-500)
    public boolean isLocationReminderEnabled;  // Enable/disable location-based reminder
    public String locationTriggerType;  // ENTER or EXIT

    // Default constructor for Room
    public Habit() {
        this.name = "";
        this.isDeleted = false;
        this.note = "";
        this.firebaseId = null;
        this.lastSyncedAt = 0;
        this.streakCount = 0;
        this.reminderTime = null;
        this.isReminderEnabled = false;
        this.repeatPattern = RepeatPattern.DAILY.name();
        this.repeatDays = "[]";
        this.customIntervalDays = 1;
        this.priority = Priority.MEDIUM.name();
        this.category = "Other";
        this.locationName = null;
        this.latitude = null;
        this.longitude = null;
        this.locationRadius = 100;
        this.isLocationReminderEnabled = false;
        this.locationTriggerType = LocationTriggerType.ENTER.name();
    }

    // Constructor for creating new Habit with name
    @Ignore
    public Habit(String name) {
        this();
        this.name = name;
    }

    // ===========================
    // GETTERS AND SETTERS
    // ===========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getFirebaseId() {
        return firebaseId;
    }

    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }

    public long getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(long lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public int getStreakCount() {
        return streakCount;
    }

    public void setStreakCount(int streakCount) {
        this.streakCount = streakCount;
    }

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean isReminderEnabled() {
        return isReminderEnabled;
    }

    public void setReminderEnabled(boolean reminderEnabled) {
        isReminderEnabled = reminderEnabled;
    }

    public String getRepeatPattern() {
        return repeatPattern;
    }

    public void setRepeatPattern(String repeatPattern) {
        this.repeatPattern = repeatPattern;
    }

    public String getRepeatDays() {
        return repeatDays;
    }

    public void setRepeatDays(String repeatDays) {
        this.repeatDays = repeatDays;
    }

    public int getCustomIntervalDays() {
        return customIntervalDays;
    }

    public void setCustomIntervalDays(int customIntervalDays) {
        this.customIntervalDays = customIntervalDays;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // Helper methods for enums
    public Priority getPriorityEnum() {
        return Priority.fromString(this.priority);
    }

    public void setPriorityEnum(Priority priority) {
        this.priority = priority.name();
    }

    public RepeatPattern getRepeatPatternEnum() {
        return RepeatPattern.fromString(this.repeatPattern);
    }

    public void setRepeatPatternEnum(RepeatPattern pattern) {
        this.repeatPattern = pattern.name();
    }

    // Location getters and setters
    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public int getLocationRadius() {
        return locationRadius;
    }

    public void setLocationRadius(int locationRadius) {
        // Clamp radius to valid range (50-500 meters)
        this.locationRadius = Math.max(50, Math.min(500, locationRadius));
    }

    public boolean isLocationReminderEnabled() {
        return isLocationReminderEnabled;
    }

    public void setLocationReminderEnabled(boolean locationReminderEnabled) {
        isLocationReminderEnabled = locationReminderEnabled;
    }

    public String getLocationTriggerType() {
        return locationTriggerType;
    }

    public void setLocationTriggerType(String locationTriggerType) {
        this.locationTriggerType = locationTriggerType;
    }

    public LocationTriggerType getLocationTriggerTypeEnum() {
        return LocationTriggerType.fromString(this.locationTriggerType);
    }

    public void setLocationTriggerTypeEnum(LocationTriggerType triggerType) {
        this.locationTriggerType = triggerType.name();
    }

    /**
     * Check if this habit has a valid location set.
     * @return true if both latitude and longitude are set
     */
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }


    // ===========================
    // FIRESTORE SERIALIZATION
    // ===========================

    /**
     * Convert Habit to a Map for Firestore storage.
     * @return Map containing all habit fields
     */
    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name != null ? name : "");
        map.put("note", note != null ? note : "");
        map.put("isDeleted", isDeleted);
        map.put("firebaseId", firebaseId);
        map.put("lastSyncedAt", lastSyncedAt);
        map.put("streakCount", streakCount);
        map.put("reminderTime", reminderTime);
        map.put("isReminderEnabled", isReminderEnabled);
        map.put("repeatPattern", repeatPattern != null ? repeatPattern : RepeatPattern.DAILY.name());
        map.put("repeatDays", repeatDays != null ? repeatDays : "[]");
        map.put("customIntervalDays", customIntervalDays);
        map.put("priority", priority != null ? priority : Priority.MEDIUM.name());
        map.put("category", category != null ? category : "Other");
        // Location fields
        map.put("locationName", locationName);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("locationRadius", locationRadius);
        map.put("isLocationReminderEnabled", isLocationReminderEnabled);
        map.put("locationTriggerType", locationTriggerType != null ? locationTriggerType : LocationTriggerType.ENTER.name());
        map.put("createdAt", System.currentTimeMillis());
        map.put("updatedAt", System.currentTimeMillis());
        return map;
    }

    /**
     * Create a Habit from Firestore document data.
     * @param map Firestore document data
     * @return Habit object with all fields populated
     */
    public static Habit fromFirestoreMap(Map<String, Object> map) {
        Habit habit = new Habit();
        
        if (map.containsKey("id")) {
            Object idObj = map.get("id");
            if (idObj instanceof Long) {
                habit.id = ((Long) idObj).intValue();
            } else if (idObj instanceof Integer) {
                habit.id = (Integer) idObj;
            }
        }
        
        habit.name = getStringOrDefault(map, "name", "");
        habit.note = getStringOrDefault(map, "note", "");
        habit.isDeleted = getBooleanOrDefault(map, "isDeleted", false);
        habit.firebaseId = getStringOrDefault(map, "firebaseId", null);
        habit.lastSyncedAt = getLongOrDefault(map, "lastSyncedAt", 0L);
        habit.streakCount = getIntOrDefault(map, "streakCount", 0);
        habit.reminderTime = getStringOrDefault(map, "reminderTime", null);
        habit.isReminderEnabled = getBooleanOrDefault(map, "isReminderEnabled", false);
        habit.repeatPattern = getStringOrDefault(map, "repeatPattern", RepeatPattern.DAILY.name());
        habit.repeatDays = getStringOrDefault(map, "repeatDays", "[]");
        habit.customIntervalDays = getIntOrDefault(map, "customIntervalDays", 1);
        habit.priority = getStringOrDefault(map, "priority", Priority.MEDIUM.name());
        habit.category = getStringOrDefault(map, "category", "Other");
        // Location fields
        habit.locationName = getStringOrDefault(map, "locationName", null);
        habit.latitude = getDoubleOrDefault(map, "latitude", null);
        habit.longitude = getDoubleOrDefault(map, "longitude", null);
        habit.locationRadius = getIntOrDefault(map, "locationRadius", 100);
        habit.isLocationReminderEnabled = getBooleanOrDefault(map, "isLocationReminderEnabled", false);
        habit.locationTriggerType = getStringOrDefault(map, "locationTriggerType", LocationTriggerType.ENTER.name());
        
        return habit;
    }

    // Helper methods for safe type conversion
    private static String getStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    private static boolean getBooleanOrDefault(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    private static long getLongOrDefault(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return defaultValue;
    }

    private static int getIntOrDefault(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        return defaultValue;
    }

    private static Double getDoubleOrDefault(Map<String, Object> map, String key, Double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Float) {
            return ((Float) value).doubleValue();
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Habit habit = (Habit) o;
        return id == habit.id &&
                isDeleted == habit.isDeleted &&
                lastSyncedAt == habit.lastSyncedAt &&
                streakCount == habit.streakCount &&
                isReminderEnabled == habit.isReminderEnabled &&
                customIntervalDays == habit.customIntervalDays &&
                locationRadius == habit.locationRadius &&
                isLocationReminderEnabled == habit.isLocationReminderEnabled &&
                objectsEquals(name, habit.name) &&
                objectsEquals(note, habit.note) &&
                objectsEquals(firebaseId, habit.firebaseId) &&
                objectsEquals(reminderTime, habit.reminderTime) &&
                objectsEquals(repeatPattern, habit.repeatPattern) &&
                objectsEquals(repeatDays, habit.repeatDays) &&
                objectsEquals(priority, habit.priority) &&
                objectsEquals(category, habit.category) &&
                objectsEquals(locationName, habit.locationName) &&
                objectsEquals(latitude, habit.latitude) &&
                objectsEquals(longitude, habit.longitude) &&
                objectsEquals(locationTriggerType, habit.locationTriggerType);
    }

    private static boolean objectsEquals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (isDeleted ? 1 : 0);
        return result;
    }
}
