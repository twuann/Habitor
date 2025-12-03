package com.example.habitor.utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.habitor.R;
import com.example.habitor.model.Habit;
import com.example.habitor.model.HabitDao;
import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.MergeStrategy;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for handling local-to-cloud data migration when signing in.
 * Shows a dialog to let user choose how to handle existing local habits.
 * 
 * Requirement 7.3: Offer to merge local habits with cloud data when signing in.
 */
public class MergeDialogHelper {

    private static final String TAG = "MergeDialogHelper";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_HABITS = "habits";

    private final Context context;
    private final HabitDao habitDao;
    private final FirebaseFirestore firestore;

    /**
     * Callback interface for merge operation completion.
     */
    public interface OnMergeCompleteListener {
        void onMergeComplete(boolean success, String message);
    }

    /**
     * Callback interface for checking if merge is needed.
     */
    public interface OnCheckCompleteListener {
        void onCheckComplete(boolean needsMerge, int localCount, int cloudCount);
    }

    public MergeDialogHelper(Context context) {
        this.context = context.getApplicationContext();
        this.habitDao = AppDatabase.getInstance(this.context).habitDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Check if merge dialog should be shown.
     * Returns true if there are local habits without Firebase IDs (never synced).
     * 
     * @param userId Firebase UID of the signed-in user
     * @param listener Callback with result
     */
    public void checkIfMergeNeeded(String userId, OnCheckCompleteListener listener) {
        // Get local habits that have never been synced (no firebaseId)
        List<Habit> localHabits = habitDao.getAll();
        List<Habit> unsyncedLocalHabits = new ArrayList<>();
        for (Habit habit : localHabits) {
            if (habit.getFirebaseId() == null || habit.getFirebaseId().isEmpty()) {
                unsyncedLocalHabits.add(habit);
            }
        }
        
        int localCount = unsyncedLocalHabits.size();
        
        if (localCount == 0) {
            // No unsynced local habits, no merge needed
            if (listener != null) {
                listener.onCheckComplete(false, 0, 0);
            }
            return;
        }

        // Check cloud habits count
        firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_HABITS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int cloudCount = querySnapshot.size();
                    boolean needsMerge = localCount > 0;
                    if (listener != null) {
                        listener.onCheckComplete(needsMerge, localCount, cloudCount);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check cloud habits: " + e.getMessage());
                    // Assume merge is needed if we can't check cloud
                    if (listener != null) {
                        listener.onCheckComplete(localCount > 0, localCount, 0);
                    }
                });
    }


    /**
     * Show the merge dialog to let user choose how to handle existing local habits.
     * 
     * @param userId Firebase UID of the signed-in user
     * @param localCount Number of local habits
     * @param cloudCount Number of cloud habits
     * @param listener Callback for merge completion
     */
    public void showMergeDialog(Context activityContext, String userId, int localCount, int cloudCount, 
                                OnMergeCompleteListener listener) {
        View dialogView = LayoutInflater.from(activityContext)
                .inflate(R.layout.dialog_merge_habits, null);

        TextView tvLocalCount = dialogView.findViewById(R.id.tvLocalCount);
        TextView tvCloudCount = dialogView.findViewById(R.id.tvCloudCount);
        RadioGroup rgMergeOptions = dialogView.findViewById(R.id.rgMergeOptions);

        tvLocalCount.setText(activityContext.getString(R.string.local_habits_count, localCount));
        tvCloudCount.setText(activityContext.getString(R.string.cloud_habits_count, cloudCount));

        AlertDialog dialog = new AlertDialog.Builder(activityContext)
                .setView(dialogView)
                .setPositiveButton(R.string.merge_confirm, (d, which) -> {
                    MergeStrategy strategy = getSelectedStrategy(rgMergeOptions);
                    executeMerge(userId, strategy, listener);
                })
                .setNegativeButton(R.string.merge_cancel, (d, which) -> {
                    // User cancelled, just proceed without merge
                    if (listener != null) {
                        listener.onMergeComplete(true, "Merge cancelled");
                    }
                })
                .setCancelable(false)
                .create();

        dialog.show();
    }

    /**
     * Get the selected merge strategy from the radio group.
     */
    private MergeStrategy getSelectedStrategy(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.rbKeepLocal) {
            return MergeStrategy.KEEP_LOCAL;
        } else if (selectedId == R.id.rbKeepCloud) {
            return MergeStrategy.KEEP_CLOUD;
        } else {
            return MergeStrategy.MERGE_BOTH;
        }
    }

    /**
     * Execute the merge operation based on the selected strategy.
     * 
     * @param userId Firebase UID
     * @param strategy The merge strategy to use
     * @param listener Callback for completion
     */
    public void executeMerge(String userId, MergeStrategy strategy, OnMergeCompleteListener listener) {
        Log.d(TAG, "Executing merge with strategy: " + strategy);
        
        switch (strategy) {
            case KEEP_LOCAL:
                executeKeepLocal(userId, listener);
                break;
            case KEEP_CLOUD:
                executeKeepCloud(userId, listener);
                break;
            case MERGE_BOTH:
                executeMergeBoth(userId, listener);
                break;
        }
    }

    /**
     * KEEP_LOCAL strategy: Upload all local habits to cloud, replacing cloud data.
     * 
     * Requirement 7.3: Keep local habits option.
     */
    private void executeKeepLocal(String userId, OnMergeCompleteListener listener) {
        Log.d(TAG, "Executing KEEP_LOCAL strategy");
        
        List<Habit> localHabits = habitDao.getAll();
        if (localHabits.isEmpty()) {
            if (listener != null) {
                listener.onMergeComplete(true, "No local habits to upload");
            }
            return;
        }

        // First, delete all cloud habits for this user
        firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_HABITS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Delete all existing cloud habits
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                    
                    // Then upload all local habits
                    uploadLocalHabitsToCloud(userId, localHabits, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to clear cloud habits: " + e.getMessage());
                    // Try to upload anyway
                    uploadLocalHabitsToCloud(userId, localHabits, listener);
                });
    }

    /**
     * Upload local habits to cloud.
     */
    private void uploadLocalHabitsToCloud(String userId, List<Habit> habits, OnMergeCompleteListener listener) {
        if (habits.isEmpty()) {
            if (listener != null) {
                listener.onMergeComplete(true, "No habits to upload");
            }
            return;
        }

        final int[] uploadedCount = {0};
        final int totalCount = habits.size();

        for (Habit habit : habits) {
            Map<String, Object> habitMap = habit.toFirestoreMap();
            habitMap.put("updatedAt", System.currentTimeMillis());

            firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_HABITS)
                    .add(habitMap)
                    .addOnSuccessListener(documentReference -> {
                        // Update local habit with Firebase ID
                        String firebaseId = documentReference.getId();
                        habitDao.updateSyncStatus(habit.getId(), firebaseId, System.currentTimeMillis());
                        
                        uploadedCount[0]++;
                        if (uploadedCount[0] >= totalCount && listener != null) {
                            listener.onMergeComplete(true, "Uploaded " + totalCount + " habits to cloud");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload habit: " + e.getMessage());
                        uploadedCount[0]++;
                        if (uploadedCount[0] >= totalCount && listener != null) {
                            listener.onMergeComplete(false, "Some habits failed to upload");
                        }
                    });
        }
    }


    /**
     * KEEP_CLOUD strategy: Replace local habits with cloud data.
     * 
     * Requirement 7.3: Keep cloud habits option.
     */
    private void executeKeepCloud(String userId, OnMergeCompleteListener listener) {
        Log.d(TAG, "Executing KEEP_CLOUD strategy");
        
        // Fetch cloud habits
        firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_HABITS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Delete all local habits that don't have a Firebase ID
                    List<Habit> localHabits = habitDao.getAll();
                    for (Habit habit : localHabits) {
                        if (habit.getFirebaseId() == null || habit.getFirebaseId().isEmpty()) {
                            habitDao.deleteHabit(habit);
                        }
                    }
                    
                    // Import cloud habits
                    int importedCount = 0;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                Habit cloudHabit = Habit.fromFirestoreMap(data);
                                cloudHabit.setFirebaseId(doc.getId());
                                cloudHabit.setLastSyncedAt(System.currentTimeMillis());
                                
                                // Check if habit already exists locally by Firebase ID
                                Habit existingHabit = findHabitByFirebaseId(doc.getId());
                                if (existingHabit != null) {
                                    // Update existing
                                    cloudHabit.setId(existingHabit.getId());
                                    habitDao.update(cloudHabit);
                                } else {
                                    // Insert new
                                    habitDao.insert(cloudHabit);
                                }
                                importedCount++;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error importing habit: " + e.getMessage());
                        }
                    }
                    
                    if (listener != null) {
                        listener.onMergeComplete(true, "Imported " + importedCount + " habits from cloud");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch cloud habits: " + e.getMessage());
                    if (listener != null) {
                        listener.onMergeComplete(false, "Failed to fetch cloud habits: " + e.getMessage());
                    }
                });
    }

    /**
     * MERGE_BOTH strategy: Combine local and cloud habits with timestamp-based conflict resolution.
     * 
     * Requirement 7.3: Merge both option with timestamp-based conflict resolution.
     */
    private void executeMergeBoth(String userId, OnMergeCompleteListener listener) {
        Log.d(TAG, "Executing MERGE_BOTH strategy");
        
        // Get all local habits
        List<Habit> localHabits = habitDao.getAll();
        
        // Fetch cloud habits
        firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_HABITS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Build a map of cloud habits by name for conflict detection
                    Map<String, Habit> cloudHabitsByName = new HashMap<>();
                    Map<String, String> cloudFirebaseIds = new HashMap<>();
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                Habit cloudHabit = Habit.fromFirestoreMap(data);
                                cloudHabit.setFirebaseId(doc.getId());
                                cloudHabitsByName.put(cloudHabit.getName().toLowerCase(), cloudHabit);
                                cloudFirebaseIds.put(cloudHabit.getName().toLowerCase(), doc.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing cloud habit: " + e.getMessage());
                        }
                    }
                    
                    // Process local habits
                    List<Habit> habitsToUpload = new ArrayList<>();
                    
                    for (Habit localHabit : localHabits) {
                        String nameKey = localHabit.getName().toLowerCase();
                        
                        if (localHabit.getFirebaseId() != null && !localHabit.getFirebaseId().isEmpty()) {
                            // Already synced, skip
                            continue;
                        }
                        
                        Habit cloudHabit = cloudHabitsByName.get(nameKey);
                        
                        if (cloudHabit == null) {
                            // No conflict, upload local habit
                            habitsToUpload.add(localHabit);
                        } else {
                            // Conflict detected - use timestamp-based resolution
                            long localTimestamp = localHabit.getLastSyncedAt();
                            long cloudTimestamp = cloudHabit.getLastSyncedAt();
                            
                            if (localTimestamp >= cloudTimestamp) {
                                // Local is newer or same, upload local
                                habitsToUpload.add(localHabit);
                            } else {
                                // Cloud is newer, update local with cloud data
                                cloudHabit.setId(localHabit.getId());
                                cloudHabit.setLastSyncedAt(System.currentTimeMillis());
                                habitDao.update(cloudHabit);
                            }
                            
                            // Remove from cloud map so we don't import it again
                            cloudHabitsByName.remove(nameKey);
                        }
                    }
                    
                    // Import remaining cloud habits (ones that don't exist locally)
                    for (Habit cloudHabit : cloudHabitsByName.values()) {
                        // Check if already exists by Firebase ID
                        Habit existing = findHabitByFirebaseId(cloudHabit.getFirebaseId());
                        if (existing == null) {
                            cloudHabit.setLastSyncedAt(System.currentTimeMillis());
                            habitDao.insert(cloudHabit);
                        }
                    }
                    
                    // Upload local habits that need to be synced
                    if (!habitsToUpload.isEmpty()) {
                        uploadLocalHabitsToCloud(userId, habitsToUpload, listener);
                    } else {
                        if (listener != null) {
                            listener.onMergeComplete(true, "Merge completed successfully");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch cloud habits for merge: " + e.getMessage());
                    // Fall back to just uploading local habits
                    List<Habit> unsyncedHabits = new ArrayList<>();
                    for (Habit habit : localHabits) {
                        if (habit.getFirebaseId() == null || habit.getFirebaseId().isEmpty()) {
                            unsyncedHabits.add(habit);
                        }
                    }
                    uploadLocalHabitsToCloud(userId, unsyncedHabits, listener);
                });
    }

    /**
     * Find a local habit by its Firebase ID.
     */
    private Habit findHabitByFirebaseId(String firebaseId) {
        if (firebaseId == null || firebaseId.isEmpty()) {
            return null;
        }
        
        List<Habit> allHabits = habitDao.getAll();
        for (Habit habit : allHabits) {
            if (firebaseId.equals(habit.getFirebaseId())) {
                return habit;
            }
        }
        return null;
    }
}
