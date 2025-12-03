package com.example.habitor.repository;

import android.content.Context;
import android.util.Log;

import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import com.example.habitor.model.HabitDao;
import com.example.habitor.model.Priority;
import com.example.habitor.model.SyncOperation;
import com.example.habitor.utils.AuthManager;
import com.example.habitor.utils.DeviceIdHelper;
import com.example.habitor.utils.GeofenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repository class for managing Habit data with dual-write support (Room + Firestore).
 * Implements CRUD operations that sync data between local database and cloud storage.
 * 
 * Requirements: 3.1, 3.2, 3.3, 6.2, 6.4, 7.2
 */
public class HabitRepository {

    private static final String TAG = "HabitRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_HABITS = "habits";

    private final HabitDao habitDao;
    private final FirebaseFirestore firestore;
    private final Context context;
    private final GeofenceManager geofenceManager;

    public interface OnCompleteCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnHabitInsertCallback {
        void onSuccess(int habitId);
        void onFailure(Exception e);
    }

    public HabitRepository(Context context) {
        this.context = context.getApplicationContext();
        this.habitDao = AppDatabase.getInstance(this.context).habitDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.geofenceManager = new GeofenceManager(this.context);
    }

    // Constructor for testing with injected dependencies
    public HabitRepository(HabitDao habitDao, FirebaseFirestore firestore, Context context) {
        this.habitDao = habitDao;
        this.firestore = firestore;
        this.context = context;
        this.geofenceManager = new GeofenceManager(context);
    }

    // ===========================
    // USER ID AND SYNC CHECK (Requirements: 6.4, 7.2)
    // ===========================

    /**
     * Get the user ID for Firestore operations.
     * Returns Firebase UID if signed in, device ID otherwise.
     * 
     * Requirement 6.4: Use Firebase UID instead of device ID when signed in.
     *
     * @return User ID for Firestore operations
     */
    private String getUserId() {
        AuthManager authManager = AuthManager.getInstance(context);
        if (authManager.isSignedIn()) {
            String firebaseUid = authManager.getCurrentUserId();
            if (firebaseUid != null && !firebaseUid.isEmpty()) {
                return firebaseUid;
            }
        }
        return DeviceIdHelper.getDeviceUserId(context);
    }

    /**
     * Check if cloud sync should be performed.
     * Sync is only performed when user is signed in.
     * 
     * Requirement 7.2: No cloud sync when not signed in.
     *
     * @return true if sync should be performed, false otherwise
     */
    private boolean shouldSync() {
        AuthManager authManager = AuthManager.getInstance(context);
        return authManager.isSignedIn();
    }

    /**
     * Check if the device is online.
     * Requirement 6.3: Queue changes when offline.
     *
     * @return true if online, false otherwise
     */
    private boolean isOnline() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) {
            return false;
        }
        android.net.NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        if (capabilities == null) {
            return false;
        }
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }


    // ===========================
    // CREATE OPERATIONS
    // ===========================

    /**
     * Insert a new habit with dual-write to Room and Firestore.
     * Requirement 3.1: Save habit to both local Room database and Firestore within 5 seconds.
     * Requirement 6.2: Sync to Firestore when signed in.
     * Requirement 7.2: No cloud sync when not signed in.
     *
     * @param habit The habit to insert
     * @param callback Callback for completion status
     */
    public void insertHabit(Habit habit, OnHabitInsertCallback callback) {
        // First, insert into local Room database
        try {
            long localId = habitDao.insert(habit);
            habit.setId((int) localId);
            Log.d(TAG, "Habit inserted locally with id: " + localId);

            // Register geofence if location reminder is enabled
            if (habit.hasLocation() && habit.isLocationReminderEnabled()) {
                geofenceManager.registerGeofence(habit, null);
            }

            // Call callback immediately after local insert succeeds
            // Don't wait for Firestore sync
            if (callback != null) {
                callback.onSuccess((int) localId);
            }

            // Only sync to Firestore if user is signed in (Requirement 6.2, 7.2)
            if (shouldSync()) {
                // Check if device is online (Requirement 6.3)
                if (isOnline()) {
                    // Then sync to Firestore in background (fire and forget)
                    syncHabitToFirestore(habit, new OnCompleteCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Habit synced to Firestore successfully");
                        }

                        @Override
                        public void onFailure(Exception e) {
                            // Local insert succeeded, but Firestore failed
                            // Queue for later sync
                            queueOfflineOperation(SyncOperation.OPERATION_INSERT, habit);
                            Log.w(TAG, "Firestore sync failed, queued for later: " + e.getMessage());
                        }
                    });
                } else {
                    // Device is offline, queue for later sync (Requirement 6.3)
                    queueOfflineOperation(SyncOperation.OPERATION_INSERT, habit);
                    Log.d(TAG, "Device offline, queued insert for later sync");
                }
            } else {
                Log.d(TAG, "User not signed in, skipping cloud sync for insert");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert habit locally: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    // ===========================
    // UPDATE OPERATIONS
    // ===========================

    /**
     * Update an existing habit with dual-write to Room and Firestore.
     * Requirement 3.2: Update both local and cloud storage with the changes.
     * Requirement 6.2: Sync to Firestore when signed in.
     * Requirement 7.2: No cloud sync when not signed in.
     *
     * @param habit The habit to update
     * @param callback Callback for completion status
     */
    public void updateHabit(Habit habit, OnCompleteCallback callback) {
        try {
            habitDao.update(habit);
            Log.d(TAG, "Habit updated locally: " + habit.getId());

            // Update geofence registration
            if (habit.hasLocation() && habit.isLocationReminderEnabled()) {
                // Re-register geofence (will update if exists)
                geofenceManager.registerGeofence(habit, null);
            } else {
                // Remove geofence if location reminder is disabled
                geofenceManager.unregisterGeofence(habit.getId(), null);
            }

            // Call callback immediately after local update succeeds
            if (callback != null) {
                callback.onSuccess();
            }

            // Only sync to Firestore if user is signed in (Requirement 6.2, 7.2)
            if (shouldSync()) {
                // Check if device is online (Requirement 6.3)
                if (isOnline()) {
                    // Then sync to Firestore in background
                    syncHabitToFirestore(habit, new OnCompleteCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Habit update synced to Firestore successfully");
                        }

                        @Override
                        public void onFailure(Exception e) {
                            queueOfflineOperation(SyncOperation.OPERATION_UPDATE, habit);
                            Log.w(TAG, "Firestore update failed, queued for later: " + e.getMessage());
                        }
                    });
                } else {
                    // Device is offline, queue for later sync (Requirement 6.3)
                    queueOfflineOperation(SyncOperation.OPERATION_UPDATE, habit);
                    Log.d(TAG, "Device offline, queued update for later sync");
                }
            } else {
                Log.d(TAG, "User not signed in, skipping cloud sync for update");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update habit locally: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    // ===========================
    // DELETE OPERATIONS
    // ===========================

    /**
     * Delete a habit (soft delete) with dual-write to Room and Firestore.
     * Requirement 3.3: Mark the habit as deleted in both local and cloud storage.
     * Requirement 6.2: Sync to Firestore when signed in.
     * Requirement 7.2: No cloud sync when not signed in.
     *
     * @param habitId The ID of the habit to delete
     * @param callback Callback for completion status
     */
    public void deleteHabit(int habitId, OnCompleteCallback callback) {
        try {
            Habit habit = habitDao.getHabitById(habitId);
            if (habit == null) {
                if (callback != null) {
                    callback.onFailure(new Exception("Habit not found"));
                }
                return;
            }

            // Soft delete locally
            habitDao.moveToTrash(habitId);
            habit.setDeleted(true);
            Log.d(TAG, "Habit soft-deleted locally: " + habitId);

            // Remove geofence when habit is deleted
            geofenceManager.unregisterGeofence(habitId, null);

            // Only sync to Firestore if user is signed in (Requirement 6.2, 7.2)
            if (shouldSync()) {
                // Check if device is online (Requirement 6.3)
                if (isOnline()) {
                    // Sync deletion to Firestore
                    syncHabitToFirestore(habit, new OnCompleteCallback() {
                        @Override
                        public void onSuccess() {
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            queueOfflineOperation(SyncOperation.OPERATION_DELETE, habit);
                            Log.w(TAG, "Firestore delete failed, queued for later: " + e.getMessage());
                            if (callback != null) {
                                callback.onSuccess(); // Local delete succeeded
                            }
                        }
                    });
                } else {
                    // Device is offline, queue for later sync (Requirement 6.3)
                    queueOfflineOperation(SyncOperation.OPERATION_DELETE, habit);
                    Log.d(TAG, "Device offline, queued delete for later sync");
                    if (callback != null) {
                        callback.onSuccess(); // Local delete succeeded
                    }
                }
            } else {
                Log.d(TAG, "User not signed in, skipping cloud sync for delete");
                if (callback != null) {
                    callback.onSuccess();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete habit locally: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }


    // ===========================
    // READ OPERATIONS
    // ===========================

    /**
     * Get all non-deleted habits from local database.
     *
     * @return List of all active habits
     */
    public List<Habit> getAllHabits() {
        return habitDao.getAll();
    }

    /**
     * Get habits filtered by category.
     * Requirement 10.4: Allow users to filter by one or multiple categories.
     *
     * @param category The category to filter by
     * @return List of habits in the specified category
     */
    public List<Habit> getHabitsByCategory(String category) {
        if (category == null || category.isEmpty()) {
            return getAllHabits();
        }
        return habitDao.getHabitsByCategory(category);
    }

    /**
     * Get habits filtered by priority.
     * Requirement 9.4: Allow users to filter by priority level.
     *
     * @param priority The priority to filter by
     * @return List of habits with the specified priority
     */
    public List<Habit> getHabitsByPriority(Priority priority) {
        if (priority == null) {
            return getAllHabits();
        }
        return habitDao.getHabitsByPriority(priority.name());
    }

    /**
     * Get all habits sorted by priority (HIGH first, then MEDIUM, then LOW).
     * Requirement 9.2: Sort habits by priority level.
     *
     * @return List of habits sorted by priority
     */
    public List<Habit> getAllHabitsSortedByPriority() {
        return habitDao.getAllHabitsSortedByPriority();
    }

    /**
     * Get a single habit by ID.
     *
     * @param habitId The ID of the habit
     * @return The habit, or null if not found
     */
    public Habit getHabitById(int habitId) {
        return habitDao.getHabitById(habitId);
    }

    /**
     * Get all habits in trash (soft-deleted).
     *
     * @return List of deleted habits
     */
    public List<Habit> getTrash() {
        return habitDao.getTrash();
    }

    // ===========================
    // FIRESTORE SYNC HELPERS
    // ===========================

    /**
     * Sync a habit to Firestore.
     * Requirement 6.4: Use Firebase UID when signed in.
     */
    private void syncHabitToFirestore(Habit habit, OnCompleteCallback callback) {
        String documentId = habit.getFirebaseId();
        Map<String, Object> habitMap = habit.toFirestoreMap();
        habitMap.put("updatedAt", System.currentTimeMillis());

        String currentUserId = getUserId();
        DocumentReference docRef;
        if (documentId != null && !documentId.isEmpty()) {
            // Update existing document
            docRef = firestore.collection(COLLECTION_USERS)
                    .document(currentUserId)
                    .collection(COLLECTION_HABITS)
                    .document(documentId);
        } else {
            // Create new document
            docRef = firestore.collection(COLLECTION_USERS)
                    .document(currentUserId)
                    .collection(COLLECTION_HABITS)
                    .document();
        }

        docRef.set(habitMap)
                .addOnSuccessListener(aVoid -> {
                    // Update local habit with Firebase ID and sync time
                    String firebaseId = docRef.getId();
                    long syncTime = System.currentTimeMillis();
                    habitDao.updateSyncStatus(habit.getId(), firebaseId, syncTime);
                    habit.setFirebaseId(firebaseId);
                    habit.setLastSyncedAt(syncTime);
                    Log.d(TAG, "Habit synced to Firestore: " + firebaseId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync habit to Firestore: " + e.getMessage());
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    /**
     * Queue an offline operation for later sync.
     */
    private void queueOfflineOperation(String operationType, Habit habit) {
        try {
            String habitJson = habitToJson(habit);
            SyncOperation operation = new SyncOperation(operationType, habit.getId(), habitJson);
            habitDao.insertSyncOperation(operation);
            Log.d(TAG, "Queued offline operation: " + operationType + " for habit " + habit.getId());
        } catch (Exception e) {
            Log.e(TAG, "Failed to queue offline operation: " + e.getMessage());
        }
    }

    /**
     * Simple JSON serialization for habit (for offline queue).
     */
    private String habitToJson(Habit habit) {
        Map<String, Object> map = habit.toFirestoreMap();
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(",");
            first = false;
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                json.append("null");
            } else if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Boolean || value instanceof Number) {
                json.append(value);
            } else {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ===========================
    // RESTORE OPERATIONS
    // ===========================

    /**
     * Restore a habit from trash.
     * Requirement 6.2: Sync to Firestore when signed in.
     * Requirement 7.2: No cloud sync when not signed in.
     *
     * @param habitId The ID of the habit to restore
     * @param callback Callback for completion status
     */
    public void restoreHabit(int habitId, OnCompleteCallback callback) {
        try {
            habitDao.restoreHabit(habitId);
            Habit habit = habitDao.getHabitById(habitId);
            if (habit != null && shouldSync()) {
                syncHabitToFirestore(habit, callback);
            } else if (callback != null) {
                callback.onSuccess();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore habit: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    /**
     * Permanently delete a habit.
     * Requirement 6.2: Sync to Firestore when signed in.
     * Requirement 7.2: No cloud sync when not signed in.
     *
     * @param habit The habit to permanently delete
     * @param callback Callback for completion status
     */
    public void permanentlyDeleteHabit(Habit habit, OnCompleteCallback callback) {
        try {
            habitDao.deleteHabit(habit);
            
            // Only delete from Firestore if signed in and has a Firebase ID
            if (shouldSync() && habit.getFirebaseId() != null && !habit.getFirebaseId().isEmpty()) {
                String currentUserId = getUserId();
                firestore.collection(COLLECTION_USERS)
                        .document(currentUserId)
                        .collection(COLLECTION_HABITS)
                        .document(habit.getFirebaseId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            if (callback != null) callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Failed to delete from Firestore: " + e.getMessage());
                            if (callback != null) callback.onSuccess(); // Local delete succeeded
                        });
            } else if (callback != null) {
                callback.onSuccess();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to permanently delete habit: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }
}
