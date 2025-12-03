package com.example.habitor.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Handler for location permission requests with proper rationale dialogs.
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
public class LocationPermissionHandler {

    public static final int REQUEST_FOREGROUND_LOCATION = 1001;
    public static final int REQUEST_BACKGROUND_LOCATION = 1002;

    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    private final Activity activity;
    private PermissionCallback callback;

    public LocationPermissionHandler(Activity activity) {
        this.activity = activity;
    }

    /**
     * Check and request foreground location permission.
     */
    public void checkAndRequestForegroundPermission(PermissionCallback callback) {
        this.callback = callback;

        if (hasForegroundLocationPermission()) {
            callback.onPermissionGranted();
            return;
        }

        // Check if we should show rationale
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            showForegroundPermissionRationale();
        } else {
            requestForegroundPermission();
        }
    }

    /**
     * Check and request background location permission (Android 10+).
     */
    public void checkAndRequestBackgroundPermission(PermissionCallback callback) {
        this.callback = callback;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Background location not needed for Android 9 and below
            callback.onPermissionGranted();
            return;
        }

        if (hasBackgroundLocationPermission()) {
            callback.onPermissionGranted();
            return;
        }

        // Must have foreground permission first
        if (!hasForegroundLocationPermission()) {
            callback.onPermissionDenied();
            return;
        }

        // Check if we should show rationale
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            showBackgroundPermissionRationale();
        } else {
            requestBackgroundPermission();
        }
    }

    private boolean hasForegroundLocationPermission() {
        return ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void showForegroundPermissionRationale() {
        new AlertDialog.Builder(activity)
                .setTitle("Location Permission Required")
                .setMessage("Habitor needs location access to:\n\n" +
                        "• Show your habit locations on the map\n" +
                        "• Help you pick locations for your habits\n\n" +
                        "Your location data is only used within the app and is never shared.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    requestForegroundPermission();
                })
                .setNegativeButton("Not Now", (dialog, which) -> {
                    if (callback != null) {
                        callback.onPermissionDenied();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showBackgroundPermissionRationale() {
        new AlertDialog.Builder(activity)
                .setTitle("Background Location Required")
                .setMessage("To receive reminders when you arrive at or leave a location, " +
                        "Habitor needs access to your location in the background.\n\n" +
                        "Please select \"Allow all the time\" in the next screen.\n\n" +
                        "Your location is only used for habit reminders and is never shared.")
                .setPositiveButton("Continue", (dialog, which) -> {
                    requestBackgroundPermission();
                })
                .setNegativeButton("Not Now", (dialog, which) -> {
                    if (callback != null) {
                        callback.onPermissionDenied();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void requestForegroundPermission() {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_FOREGROUND_LOCATION);
    }

    private void requestBackgroundPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_BACKGROUND_LOCATION);
        }
    }

    /**
     * Handle permission result from Activity.onRequestPermissionsResult().
     */
    public void handlePermissionResult(int requestCode, @NonNull String[] permissions,
                                       @NonNull int[] grantResults) {
        if (callback == null) return;

        if (requestCode == REQUEST_FOREGROUND_LOCATION) {
            if (grantResults.length > 0 && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callback.onPermissionGranted();
            } else {
                callback.onPermissionDenied();
            }
        } else if (requestCode == REQUEST_BACKGROUND_LOCATION) {
            if (grantResults.length > 0 && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callback.onPermissionGranted();
            } else {
                callback.onPermissionDenied();
            }
        }
    }

    /**
     * Show dialog explaining limited functionality when permission is denied.
     */
    public void showPermissionDeniedExplanation() {
        new AlertDialog.Builder(activity)
                .setTitle("Limited Functionality")
                .setMessage("Without location permission, you can still create habits " +
                        "but location-based features will be disabled.\n\n" +
                        "You can enable location permission later in Settings.")
                .setPositiveButton("OK", null)
                .show();
    }
}
