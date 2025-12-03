package com.example.habitor.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Habit;
import com.example.habitor.model.HabitDao;
import com.example.habitor.model.SyncOperation;
import com.example.habitor.utils.AuthManager;
import com.example.habitor.utils.DeviceIdHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Map;

/**
 * Manager class for handling data synchronization between local Room database and Firebase Firestore.
 * Implements offline-first architecture with queue-based sync for offline changes.
 * 
 * Requirements: 3.4, 3.5, 6.2, 6.3, 6.4, 7.2
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_HABITS = "habits";

    private final HabitDao habitDao;
    private final FirebaseFirestore firestore;
    private final ConnectivityManager connectivityManager;
    private final Context context;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isNetworkCallbackRegistered = false;

    public interface OnSyncCompleteListener {
        void onSyncComplete(boolean success, String message);
    }

    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.habitDao = AppDatabase.getInstance(this.context).habitDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    // Constructor for testing with injected dependencies
    public SyncManager(HabitDao habitDao, FirebaseFirestore firestore, 
                       ConnectivityManager connectivityManager, Context context) {
        this.habitDao = habitDao;
        this.firestore = firestore;
        this.connectivityManager = connectivityManager;
        this.context = context;
    }

    // ===========================
    // USER ID MANAGEMENT (Requirement 6.4)
    // ===========================

    /**
     * Get the user ID for sync operations.
     * Returns Firebase UID if signed in, device ID otherwise.
     * 
     * Requirement 6.4: Use Firebase UID instead of device ID for data association when signed in.
     *
     * @return User ID for Firestore operations
     */
    public String getUserId() {
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
     * Check if sync should be performed.
     * Sync is only performed when user is signed in and device is online.
     * 
     * Requirement 7.2: No cloud sync when not signed in.
     *
     * @return true if sync should be performed, false otherwise
     */
    public boolean shouldSync() {
        AuthManager authManager = AuthManager.getInstance(context);
        return authManager.isSignedIn() && isOnline();
    }


    // ===========================
    // CONNECTIVITY CHECK
    // ===========================

    /**
     * Check if the device has an active internet connection.
     * Requirement 3.5: Detect offline state for queuing changes.
     *
     * @return true if online, false if offline
     */
    public boolean isOnline() {
        if (connectivityManager == null) {
            return false;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return false;
        }

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    // ===========================
    // APP LAUNCH SYNC
    // ===========================

    /**
     * Sync habits from Firestore to local storage on app launch.
     * Requirement 3.4: Sync habits from Firestore to local storage using the user ID.
     * Requirement 6.4: Use Firebase UID when signed in.
     *
     * @param listener Callback for sync completion
     */
    public void syncOnAppLaunch(OnSyncCompleteListener listener) {
        if (!shouldSync()) {
            Log.d(TAG, "Sync not required (not signed in or offline)");
            if (listener != null) {
                listener.onSyncComplete(false, "Sync not required");
            }
            return;
        }

        String currentUserId = getUserId();
        Log.d(TAG, "Starting app launch sync for user: " + currentUserId);

        // First, process any pending offline operations
        processOfflineQueue(new OnSyncCompleteListener() {
            @Override
            public void onSyncComplete(boolean success, String message) {
                // Then fetch latest data from Firestore
                fetchHabitsFromFirestore(listener);
            }
        });
    }

    /**
     * Fetch habits from Firestore and merge with local database.
     */
    private void fetchHabitsFromFirestore(OnSyncCompleteListener listener) {
        String currentUserId = getUserId();
        firestore.collection(COLLECTION_USERS)
                .document(currentUserId)
                .collection(COLLECTION_HABITS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int syncedCount = 0;
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        try {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                Habit cloudHabit = Habit.fromFirestoreMap(data);
                                cloudHabit.setFirebaseId(document.getId());
                                mergeHabitFromCloud(cloudHabit);
                                syncedCount++;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing habit from Firestore: " + e.getMessage());
                        }
                    }
                    Log.d(TAG, "Synced " + syncedCount + " habits from Firestore");
                    if (listener != null) {
                        listener.onSyncComplete(true, "Synced " + syncedCount + " habits");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch habits from Firestore: " + e.getMessage());
                    if (listener != null) {
                        listener.onSyncComplete(false, "Sync failed: " + e.getMessage());
                    }
                });
    }

    /**
     * Merge a habit from cloud with local database.
     * Uses last-write-wins strategy based on lastSyncedAt timestamp.
     */
    private void mergeHabitFromCloud(Habit cloudHabit) {
        // Find local habit by Firebase ID
        List<Habit> localHabits = habitDao.getAll();
        Habit localHabit = null;
        
        for (Habit habit : localHabits) {
            if (cloudHabit.getFirebaseId() != null && 
                cloudHabit.getFirebaseId().equals(habit.getFirebaseId())) {
                localHabit = habit;
                break;
            }
        }

        if (localHabit == null) {
            // New habit from cloud, insert locally
            cloudHabit.setLastSyncedAt(System.currentTimeMillis());
            habitDao.insert(cloudHabit);
            Log.d(TAG, "Inserted new habit from cloud: " + cloudHabit.getName());
        } else {
            // Existing habit, check which is newer
            if (cloudHabit.getLastSyncedAt() > localHabit.getLastSyncedAt()) {
                // Cloud is newer, update local
                cloudHabit.setId(localHabit.getId());
                cloudHabit.setLastSyncedAt(System.currentTimeMillis());
                habitDao.update(cloudHabit);
                Log.d(TAG, "Updated local habit from cloud: " + cloudHabit.getName());
            }
            // If local is newer or same, keep local version
        }
    }


    // ===========================
    // OFFLINE QUEUE OPERATIONS
    // ===========================

    /**
     * Queue a change for later sync when offline.
     * Requirement 3.5: Queue changes locally when offline.
     *
     * @param operation The sync operation to queue
     */
    public void queueOfflineChange(SyncOperation operation) {
        try {
            habitDao.insertSyncOperation(operation);
            Log.d(TAG, "Queued offline operation: " + operation.getOperationType() + 
                       " for habit " + operation.getHabitId());
        } catch (Exception e) {
            Log.e(TAG, "Failed to queue offline operation: " + e.getMessage());
        }
    }

    /**
     * Process all pending offline operations.
     * Requirement 3.5: Sync queued changes when connectivity is restored.
     *
     * @param listener Callback for completion
     */
    public void processOfflineQueue(OnSyncCompleteListener listener) {
        if (!isOnline()) {
            Log.d(TAG, "Device is offline, cannot process queue");
            if (listener != null) {
                listener.onSyncComplete(false, "Device is offline");
            }
            return;
        }

        List<SyncOperation> pendingOperations = habitDao.getAllSyncOperations();
        if (pendingOperations.isEmpty()) {
            Log.d(TAG, "No pending offline operations");
            if (listener != null) {
                listener.onSyncComplete(true, "No pending operations");
            }
            return;
        }

        Log.d(TAG, "Processing " + pendingOperations.size() + " offline operations");
        processNextOperation(pendingOperations, 0, listener);
    }

    /**
     * Process operations one by one recursively.
     */
    private void processNextOperation(List<SyncOperation> operations, int index, 
                                       OnSyncCompleteListener listener) {
        if (index >= operations.size()) {
            Log.d(TAG, "Finished processing all offline operations");
            if (listener != null) {
                listener.onSyncComplete(true, "Processed " + operations.size() + " operations");
            }
            return;
        }

        SyncOperation operation = operations.get(index);
        processSingleOperation(operation, new OnSyncCompleteListener() {
            @Override
            public void onSyncComplete(boolean success, String message) {
                if (success) {
                    // Remove processed operation from queue
                    try {
                        habitDao.deleteSyncOperation(operation);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to remove operation from queue: " + e.getMessage());
                    }
                }
                // Continue with next operation regardless of success
                processNextOperation(operations, index + 1, listener);
            }
        });
    }

    /**
     * Process a single sync operation.
     */
    private void processSingleOperation(SyncOperation operation, OnSyncCompleteListener listener) {
        Habit habit = habitDao.getHabitById(operation.getHabitId());
        
        if (habit == null && !SyncOperation.OPERATION_DELETE.equals(operation.getOperationType())) {
            // Habit no longer exists locally and it's not a delete operation
            Log.w(TAG, "Habit not found for operation: " + operation.getHabitId());
            if (listener != null) {
                listener.onSyncComplete(true, "Habit not found, skipping");
            }
            return;
        }

        switch (operation.getOperationType()) {
            case SyncOperation.OPERATION_INSERT:
            case SyncOperation.OPERATION_UPDATE:
                syncHabitToFirestore(habit, listener);
                break;
            case SyncOperation.OPERATION_DELETE:
                if (habit != null && habit.getFirebaseId() != null) {
                    deleteHabitFromFirestore(habit.getFirebaseId(), listener);
                } else {
                    // Try to parse Firebase ID from stored JSON
                    String firebaseId = extractFirebaseIdFromJson(operation.getHabitJson());
                    if (firebaseId != null) {
                        deleteHabitFromFirestore(firebaseId, listener);
                    } else if (listener != null) {
                        listener.onSyncComplete(true, "No Firebase ID for delete");
                    }
                }
                break;
            default:
                Log.w(TAG, "Unknown operation type: " + operation.getOperationType());
                if (listener != null) {
                    listener.onSyncComplete(true, "Unknown operation type");
                }
        }
    }

    /**
     * Sync a habit to Firestore.
     */
    private void syncHabitToFirestore(Habit habit, OnSyncCompleteListener listener) {
        Map<String, Object> habitMap = habit.toFirestoreMap();
        habitMap.put("updatedAt", System.currentTimeMillis());

        String documentId = habit.getFirebaseId();
        String currentUserId = getUserId();
        
        if (documentId != null && !documentId.isEmpty()) {
            // Update existing document
            firestore.collection(COLLECTION_USERS)
                    .document(currentUserId)
                    .collection(COLLECTION_HABITS)
                    .document(documentId)
                    .set(habitMap)
                    .addOnSuccessListener(aVoid -> {
                        updateLocalSyncStatus(habit.getId(), documentId);
                        if (listener != null) {
                            listener.onSyncComplete(true, "Updated in Firestore");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update in Firestore: " + e.getMessage());
                        if (listener != null) {
                            listener.onSyncComplete(false, e.getMessage());
                        }
                    });
        } else {
            // Create new document
            firestore.collection(COLLECTION_USERS)
                    .document(currentUserId)
                    .collection(COLLECTION_HABITS)
                    .add(habitMap)
                    .addOnSuccessListener(documentReference -> {
                        String newFirebaseId = documentReference.getId();
                        updateLocalSyncStatus(habit.getId(), newFirebaseId);
                        if (listener != null) {
                            listener.onSyncComplete(true, "Created in Firestore");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to create in Firestore: " + e.getMessage());
                        if (listener != null) {
                            listener.onSyncComplete(false, e.getMessage());
                        }
                    });
        }
    }

    /**
     * Delete a habit from Firestore.
     */
    private void deleteHabitFromFirestore(String firebaseId, OnSyncCompleteListener listener) {
        String currentUserId = getUserId();
        firestore.collection(COLLECTION_USERS)
                .document(currentUserId)
                .collection(COLLECTION_HABITS)
                .document(firebaseId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Deleted from Firestore: " + firebaseId);
                    if (listener != null) {
                        listener.onSyncComplete(true, "Deleted from Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete from Firestore: " + e.getMessage());
                    if (listener != null) {
                        listener.onSyncComplete(false, e.getMessage());
                    }
                });
    }

    /**
     * Update local sync status after successful Firestore sync.
     */
    private void updateLocalSyncStatus(int habitId, String firebaseId) {
        try {
            habitDao.updateSyncStatus(habitId, firebaseId, System.currentTimeMillis());
        } catch (Exception e) {
            Log.e(TAG, "Failed to update local sync status: " + e.getMessage());
        }
    }

    /**
     * Extract Firebase ID from JSON string (simple parsing).
     */
    private String extractFirebaseIdFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            String key = "\"firebaseId\":\"";
            int startIndex = json.indexOf(key);
            if (startIndex == -1) {
                return null;
            }
            startIndex += key.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex == -1) {
                return null;
            }
            String firebaseId = json.substring(startIndex, endIndex);
            return firebaseId.isEmpty() || "null".equals(firebaseId) ? null : firebaseId;
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract Firebase ID from JSON: " + e.getMessage());
            return null;
        }
    }

    // ===========================
    // UTILITY METHODS
    // ===========================

    /**
     * Clear all pending sync operations.
     * Use with caution - this will discard any unsynced changes.
     */
    public void clearOfflineQueue() {
        try {
            habitDao.clearSyncQueue();
            Log.d(TAG, "Cleared offline queue");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear offline queue: " + e.getMessage());
        }
    }

    /**
     * Get count of pending sync operations.
     *
     * @return Number of pending operations
     */
    public int getPendingOperationsCount() {
        try {
            return habitDao.getAllSyncOperations().size();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get pending operations count: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Force sync all local habits to Firestore.
     * Useful for initial sync or recovery.
     *
     * @param listener Callback for completion
     */
    public void forceSyncAllHabits(OnSyncCompleteListener listener) {
        if (!isOnline()) {
            if (listener != null) {
                listener.onSyncComplete(false, "Device is offline");
            }
            return;
        }

        List<Habit> allHabits = habitDao.getAll();
        if (allHabits.isEmpty()) {
            if (listener != null) {
                listener.onSyncComplete(true, "No habits to sync");
            }
            return;
        }

        Log.d(TAG, "Force syncing " + allHabits.size() + " habits");
        forceSyncNextHabit(allHabits, 0, listener);
    }

    private void forceSyncNextHabit(List<Habit> habits, int index, OnSyncCompleteListener listener) {
        if (index >= habits.size()) {
            if (listener != null) {
                listener.onSyncComplete(true, "Synced " + habits.size() + " habits");
            }
            return;
        }

        syncHabitToFirestore(habits.get(index), new OnSyncCompleteListener() {
            @Override
            public void onSyncComplete(boolean success, String message) {
                forceSyncNextHabit(habits, index + 1, listener);
            }
        });
    }

    // ===========================
    // NETWORK CONNECTIVITY MONITORING (Requirement 6.3)
    // ===========================

    /**
     * Register a network callback to automatically process offline queue when connectivity is restored.
     * Requirement 6.3: Queue changes when offline and sync when connectivity is restored.
     */
    public void registerNetworkCallback() {
        if (connectivityManager == null || isNetworkCallbackRegistered) {
            return;
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Network available, processing offline queue");
                // Process offline queue when connectivity is restored
                if (shouldSync()) {
                    processOfflineQueue(new OnSyncCompleteListener() {
                        @Override
                        public void onSyncComplete(boolean success, String message) {
                            Log.d(TAG, "Offline queue processed: success=" + success + ", message=" + message);
                        }
                    });
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.d(TAG, "Network lost");
            }
        };

        try {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            isNetworkCallbackRegistered = true;
            Log.d(TAG, "Network callback registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register network callback: " + e.getMessage());
        }
    }

    /**
     * Unregister the network callback.
     * Should be called when the SyncManager is no longer needed.
     */
    public void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null && isNetworkCallbackRegistered) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                isNetworkCallbackRegistered = false;
                Log.d(TAG, "Network callback unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister network callback: " + e.getMessage());
            }
        }
    }

    /**
     * Check if network callback is registered.
     *
     * @return true if registered, false otherwise
     */
    public boolean isNetworkCallbackRegistered() {
        return isNetworkCallbackRegistered;
    }
}
