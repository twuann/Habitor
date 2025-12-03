package com.example.habitor.repository;

import android.content.Context;
import android.util.Log;

import com.example.habitor.model.AppDatabase;
import com.example.habitor.model.Category;
import com.example.habitor.model.HabitDao;
import com.example.habitor.utils.DeviceIdHelper;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class for managing Category data with dual-write support (Room + Firestore).
 * Implements CRUD operations that sync data between local database and cloud storage.
 * 
 * Requirements: 10.3
 */
public class CategoryRepository {

    private static final String TAG = "CategoryRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_CATEGORIES = "categories";

    private final HabitDao habitDao;
    private final FirebaseFirestore firestore;
    private final String userId;
    private final Context context;

    public interface OnCompleteCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnCategoryInsertCallback {
        void onSuccess(int categoryId);
        void onFailure(Exception e);
    }

    public interface OnCategoriesLoadCallback {
        void onSuccess(List<Category> categories);
        void onFailure(Exception e);
    }


    public CategoryRepository(Context context) {
        this.context = context.getApplicationContext();
        this.habitDao = AppDatabase.getInstance(this.context).habitDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.userId = DeviceIdHelper.getDeviceUserId(this.context);
        
        // Initialize default categories if needed
        initializeDefaultCategories();
    }

    // Constructor for testing with injected dependencies
    public CategoryRepository(HabitDao habitDao, FirebaseFirestore firestore, String userId, Context context) {
        this.habitDao = habitDao;
        this.firestore = firestore;
        this.userId = userId;
        this.context = context;
    }

    /**
     * Initialize default categories if they don't exist.
     */
    private void initializeDefaultCategories() {
        int count = habitDao.getCategoryCount();
        if (count == 0) {
            List<Category> defaults = Category.getDefaultCategories();
            for (Category category : defaults) {
                habitDao.insertCategory(category);
            }
            Log.d(TAG, "Initialized " + defaults.size() + " default categories");
        }
    }

    // ===========================
    // CREATE OPERATIONS
    // ===========================

    /**
     * Insert a new custom category with dual-write to Room and Firestore.
     * Requirement 10.3: Save custom category and make it available for future habits.
     *
     * @param category The category to insert
     * @param callback Callback for completion status
     */
    public void insertCategory(Category category, OnCategoryInsertCallback callback) {
        try {
            // Ensure custom categories are not marked as default
            category.setDefault(false);
            
            long localId = habitDao.insertCategory(category);
            category.setId((int) localId);
            Log.d(TAG, "Category inserted locally with id: " + localId);

            // Sync to Firestore
            syncCategoryToFirestore(category, new OnCompleteCallback() {
                @Override
                public void onSuccess() {
                    if (callback != null) {
                        callback.onSuccess((int) localId);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.w(TAG, "Firestore sync failed: " + e.getMessage());
                    if (callback != null) {
                        callback.onSuccess((int) localId); // Local insert succeeded
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert category locally: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }


    // ===========================
    // UPDATE OPERATIONS
    // ===========================

    /**
     * Update an existing category with dual-write to Room and Firestore.
     *
     * @param category The category to update
     * @param callback Callback for completion status
     */
    public void updateCategory(Category category, OnCompleteCallback callback) {
        try {
            habitDao.updateCategory(category);
            Log.d(TAG, "Category updated locally: " + category.getId());

            syncCategoryToFirestore(category, new OnCompleteCallback() {
                @Override
                public void onSuccess() {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.w(TAG, "Firestore update failed: " + e.getMessage());
                    if (callback != null) {
                        callback.onSuccess(); // Local update succeeded
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to update category locally: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    // ===========================
    // DELETE OPERATIONS
    // ===========================

    /**
     * Delete a custom category. Default categories cannot be deleted.
     *
     * @param category The category to delete
     * @param callback Callback for completion status
     */
    public void deleteCategory(Category category, OnCompleteCallback callback) {
        if (category.isDefault()) {
            if (callback != null) {
                callback.onFailure(new Exception("Cannot delete default categories"));
            }
            return;
        }

        try {
            habitDao.deleteCategory(category);
            Log.d(TAG, "Category deleted locally: " + category.getId());

            // Delete from Firestore
            deleteCategoryFromFirestore(category, callback);
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete category locally: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    // ===========================
    // READ OPERATIONS
    // ===========================

    /**
     * Get all categories from local database.
     *
     * @return List of all categories (default first, then custom alphabetically)
     */
    public List<Category> getAllCategories() {
        return habitDao.getAllCategories();
    }

    /**
     * Get a single category by ID.
     *
     * @param categoryId The ID of the category
     * @return The category, or null if not found
     */
    public Category getCategoryById(int categoryId) {
        return habitDao.getCategoryById(categoryId);
    }

    /**
     * Get a category by name.
     *
     * @param name The name of the category
     * @return The category, or null if not found
     */
    public Category getCategoryByName(String name) {
        return habitDao.getCategoryByName(name);
    }

    /**
     * Get only default categories.
     *
     * @return List of default categories
     */
    public List<Category> getDefaultCategories() {
        return habitDao.getDefaultCategories();
    }

    /**
     * Get only custom (user-created) categories.
     *
     * @return List of custom categories
     */
    public List<Category> getCustomCategories() {
        return habitDao.getCustomCategories();
    }

    /**
     * Get category names as a list of strings.
     *
     * @return List of category names
     */
    public List<String> getCategoryNames() {
        List<Category> categories = getAllCategories();
        List<String> names = new ArrayList<>();
        for (Category category : categories) {
            names.add(category.getName());
        }
        return names;
    }


    // ===========================
    // FIRESTORE SYNC HELPERS
    // ===========================

    /**
     * Sync a category to Firestore.
     */
    private void syncCategoryToFirestore(Category category, OnCompleteCallback callback) {
        Map<String, Object> categoryMap = categoryToMap(category);
        categoryMap.put("updatedAt", System.currentTimeMillis());

        DocumentReference docRef = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CATEGORIES)
                .document(String.valueOf(category.getId()));

        docRef.set(categoryMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Category synced to Firestore: " + category.getId());
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync category to Firestore: " + e.getMessage());
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    /**
     * Delete a category from Firestore.
     */
    private void deleteCategoryFromFirestore(Category category, OnCompleteCallback callback) {
        firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CATEGORIES)
                .document(String.valueOf(category.getId()))
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Category deleted from Firestore: " + category.getId());
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to delete category from Firestore: " + e.getMessage());
                    if (callback != null) {
                        callback.onSuccess(); // Local delete succeeded
                    }
                });
    }

    /**
     * Sync categories from Firestore to local database.
     *
     * @param callback Callback with the synced categories
     */
    public void syncFromFirestore(OnCategoriesLoadCallback callback) {
        firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CATEGORIES)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Category> cloudCategories = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Category category = categoryFromMap(doc.getData());
                        if (category != null && !category.isDefault()) {
                            // Only sync custom categories
                            Category existing = habitDao.getCategoryByName(category.getName());
                            if (existing == null) {
                                habitDao.insertCategory(category);
                            }
                            cloudCategories.add(category);
                        }
                    }
                    Log.d(TAG, "Synced " + cloudCategories.size() + " custom categories from Firestore");
                    if (callback != null) {
                        callback.onSuccess(getAllCategories());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync categories from Firestore: " + e.getMessage());
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    /**
     * Convert Category to Map for Firestore.
     */
    private Map<String, Object> categoryToMap(Category category) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", category.getId());
        map.put("name", category.getName());
        map.put("color", category.getColor());
        map.put("isDefault", category.isDefault());
        return map;
    }

    /**
     * Convert Map from Firestore to Category.
     */
    private Category categoryFromMap(Map<String, Object> map) {
        if (map == null) return null;
        
        Category category = new Category();
        
        if (map.containsKey("id")) {
            Object idObj = map.get("id");
            if (idObj instanceof Long) {
                category.setId(((Long) idObj).intValue());
            } else if (idObj instanceof Integer) {
                category.setId((Integer) idObj);
            }
        }
        
        if (map.containsKey("name")) {
            category.setName((String) map.get("name"));
        }
        
        if (map.containsKey("color")) {
            category.setColor((String) map.get("color"));
        }
        
        if (map.containsKey("isDefault")) {
            Object isDefaultObj = map.get("isDefault");
            if (isDefaultObj instanceof Boolean) {
                category.setDefault((Boolean) isDefaultObj);
            }
        }
        
        return category;
    }
}
