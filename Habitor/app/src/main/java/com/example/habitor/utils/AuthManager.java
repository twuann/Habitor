package com.example.habitor.utils;

import android.content.Context;
import android.util.Log;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habitor.sync.SyncManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages Firebase Authentication for the Habitor app.
 * Provides sign up, sign in, sign out functionality and auth state management.
 * 
 * Requirements: 1.2, 1.3, 1.4, 1.5, 1.6, 2.2, 2.3, 2.4, 2.5, 6.1
 */
public class AuthManager {

    private static final String TAG = "AuthManager";

    private static volatile AuthManager instance;
    private final Context context;
    private final FirebaseAuth firebaseAuth;
    private final List<AuthStateListener> authStateListeners;
    private final List<OnSyncCompleteListener> syncCompleteListeners;
    private final List<OnMergeNeededListener> mergeNeededListeners;

    // Minimum password length requirement
    public static final int MIN_PASSWORD_LENGTH = 6;

    // Error messages (Requirements: 1.4, 1.5, 1.6, 2.3, 2.4)
    public static final String ERROR_INVALID_EMAIL = "Invalid email format";
    public static final String ERROR_WEAK_PASSWORD = "Password must be at least 6 characters";
    public static final String ERROR_EMAIL_ALREADY_IN_USE = "Email already registered";
    public static final String ERROR_USER_NOT_FOUND = "Account not found";
    public static final String ERROR_WRONG_PASSWORD = "Incorrect password";
    public static final String ERROR_NETWORK = "No internet connection";
    public static final String ERROR_UNKNOWN = "An error occurred. Please try again.";

    /**
     * Callback interface for authentication operations.
     */
    public interface AuthCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    /**
     * Listener interface for auth state changes.
     */
    public interface AuthStateListener {
        void onAuthStateChanged(boolean isSignedIn);
    }

    /**
     * Listener interface for sync completion after sign in.
     * Requirement 6.1: Auto-sync on sign in.
     */
    public interface OnSyncCompleteListener {
        void onSyncComplete(boolean success, String message);
    }

    /**
     * Listener interface for merge dialog request.
     * Requirement 7.3: Offer to merge local habits with cloud data when signing in.
     */
    public interface OnMergeNeededListener {
        void onMergeNeeded(String userId, int localCount, int cloudCount);
        void onMergeNotNeeded();
    }


    /**
     * Private constructor for singleton pattern.
     */
    private AuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.authStateListeners = new ArrayList<>();
        this.syncCompleteListeners = new ArrayList<>();
        this.mergeNeededListeners = new ArrayList<>();

        // Set up Firebase auth state listener
        firebaseAuth.addAuthStateListener(firebaseAuth -> {
            boolean isSignedIn = firebaseAuth.getCurrentUser() != null;
            notifyAuthStateListeners(isSignedIn);
        });
    }

    /**
     * Get the singleton instance of AuthManager.
     * 
     * @param context Application context
     * @return AuthManager instance
     */
    public static AuthManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AuthManager.class) {
                if (instance == null) {
                    instance = new AuthManager(context);
                }
            }
        }
        return instance;
    }

    // ========================================
    // AUTH OPERATIONS (Requirements: 1.2, 1.3, 2.2)
    // ========================================

    /**
     * Sign up a new user with email and password.
     * Creates a new Firebase Authentication account.
     * Automatically triggers sync after successful sign up.
     * 
     * Requirements: 1.2, 1.3, 6.1
     * 
     * @param email User's email address
     * @param password User's password (minimum 6 characters)
     * @param callback Callback for success/error handling
     */
    public void signUp(@NonNull String email, @NonNull String password, @NonNull AuthCallback callback) {
        // Validate inputs first
        if (!isValidEmail(email)) {
            callback.onError(ERROR_INVALID_EMAIL);
            return;
        }
        if (!isValidPassword(password)) {
            callback.onError(ERROR_WEAK_PASSWORD);
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Account creation succeeded, user is automatically signed in
                        // Trigger auto-sync after successful sign up (Requirement 6.1)
                        triggerAutoSync();
                        callback.onSuccess();
                    } else {
                        // Handle specific error cases
                        String errorMessage = mapFirebaseError(task.getException());
                        callback.onError(errorMessage);
                    }
                });
    }

    /**
     * Sign in an existing user with email and password.
     * Automatically triggers sync after successful sign in.
     * 
     * Requirements: 2.2, 6.1
     * 
     * @param email User's email address
     * @param password User's password
     * @param callback Callback for success/error handling
     */
    public void signIn(@NonNull String email, @NonNull String password, @NonNull AuthCallback callback) {
        // Validate inputs first
        if (!isValidEmail(email)) {
            callback.onError(ERROR_INVALID_EMAIL);
            return;
        }
        if (password == null || password.isEmpty()) {
            callback.onError(ERROR_WRONG_PASSWORD);
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Trigger auto-sync after successful sign in (Requirement 6.1)
                        triggerAutoSync();
                        callback.onSuccess();
                    } else {
                        String errorMessage = mapFirebaseError(task.getException());
                        callback.onError(errorMessage);
                    }
                });
    }

    /**
     * Sign out the current user.
     */
    public void signOut() {
        firebaseAuth.signOut();
    }


    // ========================================
    // AUTH STATE (Requirements: 2.5)
    // ========================================

    /**
     * Check if a user is currently signed in.
     * 
     * @return true if user is signed in, false otherwise
     */
    public boolean isSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Get the current user's email address.
     * 
     * @return User's email, or null if not signed in
     */
    @Nullable
    public String getCurrentUserEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    /**
     * Get the current user's Firebase UID.
     * This is used for data association in Firestore.
     * 
     * @return Firebase UID, or null if not signed in
     */
    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ========================================
    // AUTH STATE LISTENERS
    // ========================================

    /**
     * Add a listener for auth state changes.
     * 
     * @param listener The listener to add
     */
    public void addAuthStateListener(@NonNull AuthStateListener listener) {
        if (!authStateListeners.contains(listener)) {
            authStateListeners.add(listener);
        }
    }

    /**
     * Remove an auth state listener.
     * 
     * @param listener The listener to remove
     */
    public void removeAuthStateListener(@NonNull AuthStateListener listener) {
        authStateListeners.remove(listener);
    }

    /**
     * Notify all listeners of auth state change.
     */
    private void notifyAuthStateListeners(boolean isSignedIn) {
        for (AuthStateListener listener : authStateListeners) {
            listener.onAuthStateChanged(isSignedIn);
        }
    }

    // ========================================
    // SYNC COMPLETE LISTENERS (Requirement 6.1)
    // ========================================

    /**
     * Add a listener for sync completion events.
     * Requirement 6.1: Notify when auto-sync completes after sign in.
     *
     * @param listener The listener to add
     */
    public void addSyncCompleteListener(@NonNull OnSyncCompleteListener listener) {
        if (!syncCompleteListeners.contains(listener)) {
            syncCompleteListeners.add(listener);
        }
    }

    /**
     * Remove a sync complete listener.
     *
     * @param listener The listener to remove
     */
    public void removeSyncCompleteListener(@NonNull OnSyncCompleteListener listener) {
        syncCompleteListeners.remove(listener);
    }

    /**
     * Notify all listeners of sync completion.
     */
    private void notifySyncCompleteListeners(boolean success, String message) {
        for (OnSyncCompleteListener listener : syncCompleteListeners) {
            listener.onSyncComplete(success, message);
        }
    }

    // ========================================
    // MERGE NEEDED LISTENERS (Requirement 7.3)
    // ========================================

    /**
     * Add a listener for merge needed events.
     * Requirement 7.3: Notify when merge dialog should be shown.
     *
     * @param listener The listener to add
     */
    public void addMergeNeededListener(@NonNull OnMergeNeededListener listener) {
        if (!mergeNeededListeners.contains(listener)) {
            mergeNeededListeners.add(listener);
        }
    }

    /**
     * Remove a merge needed listener.
     *
     * @param listener The listener to remove
     */
    public void removeMergeNeededListener(@NonNull OnMergeNeededListener listener) {
        mergeNeededListeners.remove(listener);
    }

    /**
     * Notify all listeners that merge is needed.
     */
    private void notifyMergeNeededListeners(String userId, int localCount, int cloudCount) {
        for (OnMergeNeededListener listener : mergeNeededListeners) {
            listener.onMergeNeeded(userId, localCount, cloudCount);
        }
    }

    /**
     * Notify all listeners that merge is not needed.
     */
    private void notifyMergeNotNeededListeners() {
        for (OnMergeNeededListener listener : mergeNeededListeners) {
            listener.onMergeNotNeeded();
        }
    }

    // ========================================
    // AUTO-SYNC (Requirement 6.1)
    // ========================================

    /**
     * Trigger automatic sync after successful sign in.
     * Requirement 6.1: Automatically sync habits from Firestore using the user's Firebase UID.
     * Requirement 7.3: Check if merge is needed before syncing.
     */
    private void triggerAutoSync() {
        Log.d(TAG, "Triggering auto-sync after sign in");
        
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "No user ID available for sync");
            return;
        }

        // Check if merge is needed (Requirement 7.3)
        MergeDialogHelper mergeHelper = new MergeDialogHelper(context);
        mergeHelper.checkIfMergeNeeded(userId, new MergeDialogHelper.OnCheckCompleteListener() {
            @Override
            public void onCheckComplete(boolean needsMerge, int localCount, int cloudCount) {
                if (needsMerge && !mergeNeededListeners.isEmpty()) {
                    // Notify listeners to show merge dialog
                    Log.d(TAG, "Merge needed: local=" + localCount + ", cloud=" + cloudCount);
                    notifyMergeNeededListeners(userId, localCount, cloudCount);
                } else {
                    // No merge needed, proceed with normal sync
                    Log.d(TAG, "No merge needed, proceeding with normal sync");
                    notifyMergeNotNeededListeners();
                    performAutoSync();
                }
            }
        });
    }

    /**
     * Perform the actual auto-sync operation.
     * Called after merge decision is made.
     */
    public void performAutoSync() {
        Log.d(TAG, "Performing auto-sync");
        SyncManager syncManager = new SyncManager(context);
        syncManager.syncOnAppLaunch(new SyncManager.OnSyncCompleteListener() {
            @Override
            public void onSyncComplete(boolean success, String message) {
                Log.d(TAG, "Auto-sync completed: success=" + success + ", message=" + message);
                notifySyncCompleteListeners(success, message);
            }
        });
    }

    // ========================================
    // VALIDATION (Requirements: 1.4, 1.6)
    // ========================================

    /**
     * Validate email format using Android's Patterns utility.
     * 
     * Requirements: 1.4
     * 
     * @param email Email string to validate
     * @return true if email is valid, false otherwise
     */
    public static boolean isValidEmail(@Nullable String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /**
     * Validate password meets minimum requirements.
     * 
     * Requirements: 1.6
     * 
     * @param password Password string to validate
     * @return true if password is valid (at least 6 characters), false otherwise
     */
    public static boolean isValidPassword(@Nullable String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    // ========================================
    // ERROR MAPPING (Requirements: 1.4, 1.5, 1.6, 2.3, 2.4)
    // ========================================

    /**
     * Map Firebase exceptions to user-friendly error messages.
     * 
     * @param exception The Firebase exception
     * @return User-friendly error message
     */
    public static String mapFirebaseError(@Nullable Exception exception) {
        if (exception == null) {
            return ERROR_UNKNOWN;
        }

        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            String errorCode = ((FirebaseAuthInvalidCredentialsException) exception).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(errorCode)) {
                return ERROR_INVALID_EMAIL;
            }
            // Wrong password or invalid credentials
            return ERROR_WRONG_PASSWORD;
        }

        if (exception instanceof FirebaseAuthWeakPasswordException) {
            return ERROR_WEAK_PASSWORD;
        }

        if (exception instanceof FirebaseAuthUserCollisionException) {
            return ERROR_EMAIL_ALREADY_IN_USE;
        }

        if (exception instanceof FirebaseAuthInvalidUserException) {
            return ERROR_USER_NOT_FOUND;
        }

        // Check for network errors
        String message = exception.getMessage();
        if (message != null && (message.contains("network") || message.contains("NETWORK"))) {
            return ERROR_NETWORK;
        }

        return ERROR_UNKNOWN;
    }
}
