# Requirements Document

## Introduction

Tài liệu này mô tả các yêu cầu cho tính năng Location trong ứng dụng Habitor. Tính năng này cho phép người dùng gắn vị trí địa lý cho mỗi habit, hiển thị vị trí trên bản đồ, và nhận nhắc nhở thông minh dựa trên vị trí hiện tại của người dùng (geofencing).

## Glossary

- **Habitor**: Ứng dụng theo dõi thói quen (habit tracking) trên nền tảng Android
- **Habit**: Một thói quen mà người dùng muốn theo dõi và duy trì
- **Location**: Vị trí địa lý được xác định bởi tọa độ (latitude, longitude) và tên địa điểm
- **Geofence**: Vùng địa lý ảo được xác định bởi tọa độ trung tâm và bán kính
- **Location-based Reminder**: Nhắc nhở được kích hoạt khi người dùng vào hoặc rời khỏi một geofence
- **Google Maps API**: Dịch vụ bản đồ của Google cung cấp hiển thị bản đồ và geocoding
- **Fused Location Provider**: API của Google Play Services cung cấp vị trí chính xác với tiết kiệm pin
- **Geocoding**: Quá trình chuyển đổi địa chỉ thành tọa độ và ngược lại

## Requirements

### Requirement 1: Location Data Model

**User Story:** As a developer, I want to extend the Habit data model to support location, so that habits can be associated with specific places.

#### Acceptance Criteria

1. WHEN storing a habit with location THEN the Habitor System SHALL include fields for: locationName, latitude, longitude, locationRadius, and isLocationReminderEnabled
2. WHEN serializing habit data for Firestore THEN the Habitor System SHALL include all location fields in the Map representation
3. WHEN deserializing habit data from Firestore THEN the Habitor System SHALL construct a valid Habit object with location fields populated
4. WHEN migrating from the current database schema THEN the Habitor System SHALL preserve existing habit data and set null values for new location fields

### Requirement 2: Location Picker UI

**User Story:** As a user, I want to select a location for my habit using a map, so that I can easily specify where I want to perform the habit.

#### Acceptance Criteria

1. WHEN a user taps the location field in Add/Edit Habit screen THEN the Habitor System SHALL display a full-screen map picker with search functionality
2. WHEN a user searches for a location THEN the Habitor System SHALL display autocomplete suggestions from Google Places API
3. WHEN a user selects a location from search results THEN the Habitor System SHALL center the map on that location and place a marker
4. WHEN a user long-presses on the map THEN the Habitor System SHALL place a marker at that position and reverse geocode to get the address
5. WHEN a user confirms the location selection THEN the Habitor System SHALL return the location data (name, latitude, longitude) to the habit form

### Requirement 3: Location Display in Habit

**User Story:** As a user, I want to see the location associated with my habit, so that I know where I should perform it.

#### Acceptance Criteria

1. WHEN displaying a habit card with location THEN the Habitor System SHALL show the location name with a map pin icon
2. WHEN a user opens habit detail screen THEN the Habitor System SHALL display a mini map showing the habit location with a marker
3. WHEN a user taps the mini map in habit detail THEN the Habitor System SHALL open the full map view centered on the habit location
4. WHEN a habit has no location set THEN the Habitor System SHALL display "No location set" with an option to add one

### Requirement 4: Location-based Reminders (Geofencing)

**User Story:** As a user, I want to receive reminders when I arrive at or leave a location, so that I remember to perform my habit at the right place.

#### Acceptance Criteria

1. WHEN a user enables location reminder for a habit THEN the Habitor System SHALL allow selection of trigger type: "When I arrive" or "When I leave"
2. WHEN a user configures location reminder THEN the Habitor System SHALL allow setting a radius (50m to 500m) for the geofence
3. WHEN the user enters a geofence with "When I arrive" trigger THEN the Habitor System SHALL display a notification for the associated habit
4. WHEN the user exits a geofence with "When I leave" trigger THEN the Habitor System SHALL display a notification for the associated habit
5. WHEN location permissions are not granted THEN the Habitor System SHALL prompt the user to grant permissions and explain why they are needed

### Requirement 5: Location Permission Handling

**User Story:** As a user, I want the app to request location permissions appropriately, so that I understand why location access is needed and can control it.

#### Acceptance Criteria

1. WHEN a user attempts to add a location to a habit THEN the Habitor System SHALL request ACCESS_FINE_LOCATION permission with a clear explanation
2. WHEN a user enables location-based reminders THEN the Habitor System SHALL request ACCESS_BACKGROUND_LOCATION permission (Android 10+) with explanation
3. WHEN location permission is denied THEN the Habitor System SHALL allow the habit to be saved without location and show a message explaining limited functionality
4. WHEN the user revokes location permission THEN the Habitor System SHALL disable location-based reminders and notify the user
5. WHEN displaying permission rationale THEN the Habitor System SHALL explain that location is used for reminders and map display only

### Requirement 6: Location Sync with Firebase

**User Story:** As a user, I want my habit locations to sync to the cloud, so that I can access them from any device.

#### Acceptance Criteria

1. WHEN a user saves a habit with location THEN the Habitor System SHALL sync location data to Firestore along with other habit fields
2. WHEN syncing from Firestore THEN the Habitor System SHALL restore location data and re-register geofences for enabled location reminders
3. WHEN a user updates habit location THEN the Habitor System SHALL update both local database and Firestore within 5 seconds
4. WHILE the device is offline THEN the Habitor System SHALL queue location changes and sync when connectivity is restored

### Requirement 7: Geofence Management

**User Story:** As a developer, I want efficient geofence management, so that the app performs well and respects system limits.

#### Acceptance Criteria

1. WHEN registering geofences THEN the Habitor System SHALL respect the Android limit of 100 active geofences per app
2. WHEN the device restarts THEN the Habitor System SHALL re-register all active geofences automatically
3. WHEN a habit is deleted THEN the Habitor System SHALL remove the associated geofence
4. WHEN a user disables location reminder THEN the Habitor System SHALL remove the geofence for that habit
5. WHEN geofence registration fails THEN the Habitor System SHALL log the error and notify the user with a retry option

