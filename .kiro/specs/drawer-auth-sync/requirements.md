# Requirements Document

## Introduction

Tính năng này cải thiện Navigation Drawer của ứng dụng Habitor bằng cách tích hợp hệ thống xác thực email/password với Firebase Authentication. Người dùng có thể đăng ký, đăng nhập để đồng bộ dữ liệu habit giữa các thiết bị. Drawer sẽ được đơn giản hóa, loại bỏ các mục dư thừa và gộp Device Sync với Profile thành một trải nghiệm thống nhất.

## Glossary

- **Drawer**: Navigation Drawer - thanh điều hướng bên trái của ứng dụng
- **Firebase Authentication**: Dịch vụ xác thực của Firebase hỗ trợ đăng nhập email/password
- **Sync**: Đồng bộ hóa dữ liệu habit giữa thiết bị và cloud (Firestore)
- **Auth State**: Trạng thái xác thực của người dùng (đã đăng nhập/chưa đăng nhập)
- **Nav Header**: Phần header của Navigation Drawer hiển thị thông tin người dùng

## Requirements

### Requirement 1

**User Story:** As a user, I want to sign up with email and password, so that I can create an account to sync my habits across devices.

#### Acceptance Criteria

1. WHEN a user taps the "Sign Up" button on the auth screen THEN the Habitor App SHALL display input fields for email and password
2. WHEN a user submits valid email and password (minimum 6 characters) THEN the Habitor App SHALL create a new Firebase Authentication account
3. WHEN account creation succeeds THEN the Habitor App SHALL automatically sign in the user and navigate to the home screen
4. IF account creation fails due to invalid email format THEN the Habitor App SHALL display an error message "Invalid email format"
5. IF account creation fails due to email already in use THEN the Habitor App SHALL display an error message "Email already registered"
6. IF account creation fails due to weak password THEN the Habitor App SHALL display an error message "Password must be at least 6 characters"

### Requirement 2

**User Story:** As a user, I want to sign in with my email and password, so that I can access my synced habits on any device.

#### Acceptance Criteria

1. WHEN a user taps the "Sign In" button on the auth screen THEN the Habitor App SHALL display input fields for email and password
2. WHEN a user submits correct email and password THEN the Habitor App SHALL authenticate with Firebase and navigate to the home screen
3. IF authentication fails due to wrong password THEN the Habitor App SHALL display an error message "Incorrect password"
4. IF authentication fails due to non-existent account THEN the Habitor App SHALL display an error message "Account not found"
5. WHEN a user is signed in THEN the Habitor App SHALL persist the auth state across app restarts

### Requirement 3

**User Story:** As a user, I want to see my account status in the drawer header, so that I know whether I'm signed in and syncing.

#### Acceptance Criteria

1. WHEN a user is not signed in THEN the Drawer Header SHALL display "Tap to sign in" with a default avatar
2. WHEN a user is signed in THEN the Drawer Header SHALL display the user's email and a sync status indicator
3. WHEN a user taps the drawer header while not signed in THEN the Habitor App SHALL navigate to the auth screen
4. WHEN a user taps the drawer header while signed in THEN the Habitor App SHALL navigate to the account/profile screen
5. THE Drawer Header SHALL apply proper top padding to avoid overlap with system status bar and camera notch/cutout areas

### Requirement 4

**User Story:** As a user, I want a simplified drawer menu, so that I can navigate the app more easily.

#### Acceptance Criteria

1. THE Drawer Menu SHALL contain only these items: Home, Calendar, Search, Trash, Settings
2. THE Drawer Menu SHALL remove the separate "Profile" and "Notifications" menu items
3. WHEN a user selects a menu item THEN the Habitor App SHALL navigate to the corresponding screen and close the drawer

### Requirement 5

**User Story:** As a user, I want to manage my account and sync settings in one place, so that I have a unified experience.

#### Acceptance Criteria

1. WHEN a user navigates to Settings THEN the Habitor App SHALL display account info, sync options, and notification settings in one screen
2. WHEN a user is signed in THEN the Settings Screen SHALL display a "Sign Out" button
3. WHEN a user taps "Sign Out" THEN the Habitor App SHALL sign out from Firebase and clear the auth state
4. WHEN a user is signed in THEN the Settings Screen SHALL display last sync time and a "Sync Now" button
5. WHEN a user taps "Sync Now" THEN the Habitor App SHALL trigger immediate data synchronization with Firestore

### Requirement 6

**User Story:** As a user, I want my habits to sync automatically when I sign in, so that I can access my data seamlessly.

#### Acceptance Criteria

1. WHEN a user signs in successfully THEN the Habitor App SHALL automatically sync habits from Firestore using the user's Firebase UID
2. WHEN a user creates or updates a habit while signed in THEN the Habitor App SHALL sync the change to Firestore
3. WHEN a user is offline and makes changes THEN the Habitor App SHALL queue changes and sync when connectivity is restored
4. WHEN syncing data THEN the Habitor App SHALL use the Firebase UID instead of device ID for data association

### Requirement 7

**User Story:** As a user, I want to use the app without signing in, so that I can try the app before creating an account.

#### Acceptance Criteria

1. WHEN a user opens the app without signing in THEN the Habitor App SHALL allow full local functionality
2. WHEN a user is not signed in THEN the Habitor App SHALL store habits locally only without cloud sync
3. WHEN a user signs in after using the app locally THEN the Habitor App SHALL offer to merge local habits with cloud data
