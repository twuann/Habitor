# Implementation Plan

## 1. Setup và Dependencies

- [x] 1.1 Add Location dependencies to build.gradle
  - Add play-services-location dependency
  - _Requirements: 4.1_

- [x] 1.2 Add location permissions to AndroidManifest.xml
  - Add ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION
  - _Requirements: 5.1, 5.2_

## 2. Data Model Extension

- [x] 2.1 Extend Habit model with location fields
  - Add locationName, latitude, longitude, locationRadius, isLocationReminderEnabled, locationTriggerType fields
  - Add getters/setters for new fields
  - Add hasLocation() helper method
  - Update toFirestoreMap() to include location fields
  - Update fromFirestoreMap() to parse location fields
  - _Requirements: 1.1, 1.2, 1.3_

- [ ]* 2.2 Write property test for Firestore serialization round trip
  - **Property 1: Firestore Serialization Round Trip**
  - **Validates: Requirements 1.2, 1.3**

- [x] 2.3 Create LocationTriggerType enum
  - Define ENTER and EXIT values with display names
  - Add fromString() helper method
  - _Requirements: 4.1_

- [x] 2.4 Create database migration for location fields
  - Add migration from current version to new version
  - ALTER TABLE to add all location columns with defaults
  - Update AppDatabase version and add migration
  - _Requirements: 1.4_

- [ ]* 2.5 Write unit tests for Habit location fields
  - Test default values
  - Test hasLocation() logic
  - _Requirements: 1.1_

## 3. Checkpoint - Ensure data model is correct

- [ ] 3. Checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## 4. Location Helper Utilities

- [x] 4.1 Create LocationHelper class
  - Implement hasLocationPermission() method
  - Implement hasFineLocationPermission() method
  - Implement hasBackgroundLocationPermission() method
  - Implement getCurrentLocation() with FusedLocationProviderClient
  - Implement reverseGeocode() using Geocoder
  - _Requirements: 5.1, 5.2, 2.4_

- [x] 4.2 Create LocationPermissionHandler class
  - Define permission request codes
  - Implement checkAndRequestForegroundPermission()
  - Implement checkAndRequestBackgroundPermission()
  - Implement showPermissionRationale() dialog
  - Handle permission results
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 4.3 Implement radius validation utility
  - Create validateRadius() method that clamps to 50-500 range
  - _Requirements: 4.2_

- [ ]* 4.4 Write property test for radius validation
  - **Property 2: Location Radius Validation**
  - **Validates: Requirements 4.2**

## 5. Location UI (Simplified - Current Location Only)

- [x] 5.1 Update AddEditHabitBottomSheet for location
  - Add "Get Current Location" button
  - Add location reminder toggle
  - Add trigger type selector (arrive/leave)
  - Get current location using FusedLocationProviderClient
  - Reverse geocode to get address
  - _Requirements: 2.1, 4.1_

## 6. Location Display

- [x] 6.1 Update HabitCardAdapter to show location
  - Add location name TextView with map pin icon
  - Show/hide based on hasLocation()
  - _Requirements: 3.1, 3.4_

- [ ]* 6.2 Write property test for hasLocation detection
  - **Property 4: Habit Without Location Detection**
  - **Validates: Requirements 3.4**

## 7. Checkpoint - Ensure UI components work

- [ ] 7. Checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## 8. Geofence Management

- [x] 8.1 Create GeofenceManager class
  - Initialize GeofencingClient
  - Implement buildGeofence() from Habit
  - Implement registerGeofence() with PendingIntent
  - Implement unregisterGeofence() by habitId
  - Implement reregisterAllGeofences() for boot receiver
  - Implement geofence limit enforcement (max 100)
  - _Requirements: 4.3, 4.4, 7.1, 7.2, 7.3, 7.4_

- [ ]* 8.2 Write property test for geofence limit enforcement
  - **Property 5: Geofence Limit Enforcement**
  - **Validates: Requirements 7.1**

- [ ]* 8.3 Write property test for geofence cleanup
  - **Property 6: Geofence Cleanup on Disable/Delete**
  - **Validates: Requirements 7.3, 7.4**

- [x] 8.4 Create GeofenceBroadcastReceiver
  - Handle GEOFENCE_TRANSITION_ENTER and GEOFENCE_TRANSITION_EXIT
  - Extract habit ID from geofence request ID
  - Trigger notification for matching trigger type
  - _Requirements: 4.3, 4.4_

- [ ]* 8.5 Write property test for geofence trigger correctness
  - **Property 3: Geofence Trigger Correctness**
  - **Validates: Requirements 4.3, 4.4**

- [x] 8.6 Register GeofenceBroadcastReceiver in AndroidManifest
  - Add receiver declaration
  - Add intent filter for geofence transitions
  - _Requirements: 4.3, 4.4_

- [x] 8.7 Update BootReceiver to re-register geofences
  - Call GeofenceManager.reregisterAllGeofences() on boot
  - _Requirements: 7.2_

## 9. Integration with Repository

- [x] 9.1 Update HabitRepository for location sync
  - Ensure location fields are included in Firestore sync
  - Register/unregister geofence on habit save/delete
  - _Requirements: 6.1, 6.2, 6.3, 7.3_

- [x] 9.2 Handle geofence registration on habit update
  - Unregister old geofence if location changed
  - Register new geofence if location reminder enabled
  - _Requirements: 7.3, 7.4_

## 10. Error Handling

- [x] 10.1 Implement permission denial handling
  - Allow saving habit without location on permission denial
  - Show explanation for limited functionality
  - _Requirements: 5.3, 5.4_

## 11. Final Checkpoint

- [ ] 11. Final Checkpoint
  - Ensure all tests pass, ask the user if questions arise.
