package com.example.habitor.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Helper class for location-related operations.
 * Requirements: 5.1, 5.2, 2.4
 */
public class LocationHelper {

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final Executor executor;

    public interface LocationResultCallback {
        void onLocationResult(Location location);
        void onLocationError(String error);
    }

    public interface GeocodingCallback {
        void onAddressResult(String address);
        void onGeocodingError(String error);
    }

    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Check if the app has fine location permission.
     */
    public boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if the app has coarse location permission.
     */
    public boolean hasCoarseLocationPermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if the app has any location permission (fine or coarse).
     */
    public boolean hasLocationPermission() {
        return hasFineLocationPermission() || hasCoarseLocationPermission();
    }

    /**
     * Check if the app has background location permission (Android 10+).
     */
    public boolean hasBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        // Background location not required for Android 9 and below
        return true;
    }

    /**
     * Get the current location using FusedLocationProviderClient.
     */
    public void getCurrentLocation(LocationResultCallback callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Location permission not granted");
            return;
        }

        try {
            // First try to get last known location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            callback.onLocationResult(location);
                        } else {
                            // Request fresh location if last location is null
                            requestFreshLocation(callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        callback.onLocationError("Failed to get location: " + e.getMessage());
                    });
        } catch (SecurityException e) {
            callback.onLocationError("Location permission denied");
        }
    }

    private void requestFreshLocation(LocationResultCallback callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Location permission not granted");
            return;
        }

        try {
            LocationRequest locationRequest = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(5000)
                    .setMaxUpdates(1)
                    .build();

            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        callback.onLocationResult(location);
                    } else {
                        callback.onLocationError("Unable to get location");
                    }
                    fusedLocationClient.removeLocationUpdates(this);
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, 
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            callback.onLocationError("Location permission denied");
        }
    }

    /**
     * Reverse geocode coordinates to get a human-readable address.
     */
    public void reverseGeocode(double latitude, double longitude, GeocodingCallback callback) {
        executor.execute(() -> {
            if (!Geocoder.isPresent()) {
                callback.onGeocodingError("Geocoder not available");
                return;
            }

            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String addressText = formatAddress(address);
                    callback.onAddressResult(addressText);
                } else {
                    callback.onGeocodingError("No address found");
                }
            } catch (IOException e) {
                callback.onGeocodingError("Geocoding failed: " + e.getMessage());
            }
        });
    }

    private String formatAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        
        // Try to get a meaningful name
        if (address.getFeatureName() != null && !address.getFeatureName().isEmpty()) {
            sb.append(address.getFeatureName());
        }
        
        // Add locality if different from feature name
        if (address.getLocality() != null && !address.getLocality().isEmpty()) {
            if (sb.length() > 0 && !sb.toString().equals(address.getLocality())) {
                sb.append(", ");
            }
            if (!sb.toString().contains(address.getLocality())) {
                sb.append(address.getLocality());
            }
        }
        
        // Fallback to address line if nothing else
        if (sb.length() == 0 && address.getAddressLine(0) != null) {
            sb.append(address.getAddressLine(0));
        }
        
        return sb.length() > 0 ? sb.toString() : "Unknown location";
    }

    /**
     * Validate and clamp radius to valid range (50-500 meters).
     */
    public static int validateRadius(int radius) {
        return Math.max(50, Math.min(500, radius));
    }
}
