# Requirements Document

## Introduction

Tài liệu này mô tả các yêu cầu nâng cấp cho ứng dụng Habitor - một habit tracking app trên Android. Phạm vi nâng cấp bao gồm: tích hợp Firebase làm backend, cải thiện UI/UX, và bổ sung các tính năng nhắc nhở thông minh với khả năng hẹn giờ và lặp lại theo lịch.

## Glossary

- **Habitor**: Ứng dụng theo dõi thói quen (habit tracking) trên nền tảng Android
- **Firebase**: Nền tảng backend-as-a-service của Google cung cấp Authentication, Firestore Database, và Cloud Messaging
- **Habit**: Một thói quen mà người dùng muốn theo dõi và duy trì
- **Reminder**: Thông báo nhắc nhở người dùng thực hiện habit vào thời điểm đã đặt
- **Repeat Pattern**: Mẫu lặp lại cho reminder (hàng ngày, hàng tuần, theo ngày cụ thể)
- **Push Notification**: Thông báo đẩy từ server đến thiết bị người dùng
- **FCM (Firebase Cloud Messaging)**: Dịch vụ gửi push notification của Firebase
- **Firestore**: Cơ sở dữ liệu NoSQL realtime của Firebase
- **Streak**: Chuỗi ngày liên tiếp hoàn thành habit

## Requirements

### Requirement 1: Firebase Setup và Documentation

**User Story:** As a developer, I want clear Firebase setup documentation, so that I can integrate Firebase services into the Habitor app efficiently.

#### Acceptance Criteria

1. WHEN a developer opens the Firebase setup guide THEN the Habitor System SHALL provide step-by-step instructions for creating a Firebase project and linking it to Android Studio
2. WHEN setting up Firestore Database THEN the Habitor System SHALL provide database schema documentation and security rules configuration
3. WHEN integrating FCM THEN the Habitor System SHALL include instructions for obtaining and configuring the server key and client SDK
4. WHEN adding Firebase dependencies THEN the Habitor System SHALL document required gradle configurations and google-services.json setup

### Requirement 2: User Onboarding Enhancement

**User Story:** As a user, I want to complete onboarding with my profile information, so that the app can personalize my experience.

#### Acceptance Criteria

1. WHEN a user opens the app for the first time THEN the Habitor System SHALL display the onboarding screen requesting name, age, and gender
2. WHEN a user completes onboarding THEN the Habitor System SHALL generate a unique device-based user ID and save it to SharedPreferences
3. WHEN a user completes onboarding THEN the Habitor System SHALL navigate to the home screen and initialize local database
4. WHEN the app launches after onboarding THEN the Habitor System SHALL skip onboarding and load the home screen directly
5. WHEN a user updates profile information THEN the Habitor System SHALL save changes to SharedPreferences and sync to Firestore

### Requirement 3: Cloud Data Synchronization

**User Story:** As a user, I want my habits to sync to the cloud, so that I can access them from any device and never lose my data.

#### Acceptance Criteria

1. WHEN a user creates a new habit THEN the Habitor System SHALL save the habit to both local Room database and Firestore within 5 seconds
2. WHEN a user modifies a habit THEN the Habitor System SHALL update both local and cloud storage with the changes
3. WHEN a user deletes a habit THEN the Habitor System SHALL mark the habit as deleted in both local and cloud storage
4. WHEN the app launches after onboarding THEN the Habitor System SHALL sync habits from Firestore to local storage using the device user ID
5. WHILE the device is offline THEN the Habitor System SHALL queue changes locally and sync when connectivity is restored

### Requirement 4: Habit Reminder Scheduling

**User Story:** As a user, I want to set reminders for my habits, so that I receive notifications at specific times to help me stay consistent.

#### Acceptance Criteria

1. WHEN a user creates or edits a habit THEN the Habitor System SHALL provide options to set reminder time (hour and minute)
2. WHEN a user sets a reminder time THEN the Habitor System SHALL schedule a local alarm using AlarmManager for that specific time
3. WHEN the scheduled alarm triggers THEN the Habitor System SHALL display a push notification with the habit name and motivational message
4. WHEN a user disables a reminder THEN the Habitor System SHALL cancel the scheduled alarm for that habit
5. WHEN the device restarts THEN the Habitor System SHALL reschedule all active reminders automatically

### Requirement 5: Repeat Pattern Configuration

**User Story:** As a user, I want to configure repeat patterns for my reminders, so that I only get notified on days relevant to each habit.

#### Acceptance Criteria

1. WHEN a user configures a reminder THEN the Habitor System SHALL offer repeat options: Daily, Weekly (specific days), and Custom interval
2. WHEN a user selects Daily repeat THEN the Habitor System SHALL schedule the reminder to trigger every day at the set time
3. WHEN a user selects Weekly repeat THEN the Habitor System SHALL allow selection of specific days (Monday through Sunday) and schedule accordingly
4. WHEN a user selects Custom interval THEN the Habitor System SHALL allow input of interval in days (e.g., every 3 days)
5. WHEN displaying habit details THEN the Habitor System SHALL show the configured repeat pattern in human-readable format

### Requirement 6: Push Notification Enhancement

**User Story:** As a user, I want rich and actionable notifications, so that I can quickly interact with my habits without opening the app.

#### Acceptance Criteria

1. WHEN a reminder notification is displayed THEN the Habitor System SHALL include action buttons for "Mark Complete" and "Snooze"
2. WHEN a user taps "Mark Complete" on a notification THEN the Habitor System SHALL record the habit completion for the current date
3. WHEN a user taps "Snooze" on a notification THEN the Habitor System SHALL reschedule the reminder for 10 minutes later
4. WHEN a user taps the notification body THEN the Habitor System SHALL open the app and navigate to the specific habit detail screen
5. WHEN multiple habit reminders are due THEN the Habitor System SHALL group notifications with a summary showing the count of pending habits

### Requirement 7: UI Enhancement - Home Screen

**User Story:** As a user, I want an improved home screen, so that I can quickly see my habits and their status for today.

#### Acceptance Criteria

1. WHEN the home screen loads THEN the Habitor System SHALL display habits in a card-based layout with completion status indicators
2. WHEN displaying a habit card THEN the Habitor System SHALL show habit name, current streak count, and next reminder time
3. WHEN a user taps a habit card THEN the Habitor System SHALL toggle the completion status for today with visual feedback
4. WHEN a user long-presses a habit card THEN the Habitor System SHALL display a context menu with Edit, Delete, and Share options
5. WHEN all habits are completed for the day THEN the Habitor System SHALL display a congratulatory message with celebration animation

### Requirement 8: UI Enhancement - Habit Detail Screen

**User Story:** As a user, I want a detailed view of each habit, so that I can see my progress history and configure settings.

#### Acceptance Criteria

1. WHEN a user opens habit detail THEN the Habitor System SHALL display a calendar view showing completion history for the current month
2. WHEN displaying habit statistics THEN the Habitor System SHALL show current streak, longest streak, and completion rate percentage
3. WHEN a user edits reminder settings THEN the Habitor System SHALL provide a time picker and repeat pattern selector in a bottom sheet dialog
4. WHEN a user views the calendar THEN the Habitor System SHALL highlight completed days with a distinct color and show streak connections
5. WHEN a user swipes between months THEN the Habitor System SHALL load and display the completion history for the selected month

### Requirement 9: Habit Priority System

**User Story:** As a user, I want to set priority levels for my habits, so that I can focus on the most important ones first.

#### Acceptance Criteria

1. WHEN a user creates or edits a habit THEN the Habitor System SHALL provide priority options: High, Medium, and Low
2. WHEN displaying habits on the home screen THEN the Habitor System SHALL sort habits by priority level (High first, then Medium, then Low)
3. WHEN a habit has High priority THEN the Habitor System SHALL display a visual indicator (colored badge or icon) to distinguish it
4. WHEN filtering habits THEN the Habitor System SHALL allow users to filter by priority level
5. WHEN a High priority habit is incomplete near end of day THEN the Habitor System SHALL send an additional reminder notification

### Requirement 10: Habit Categories and Tags

**User Story:** As a user, I want to organize my habits into categories, so that I can group related habits together.

#### Acceptance Criteria

1. WHEN a user creates or edits a habit THEN the Habitor System SHALL allow selection of a category (Health, Work, Personal, Learning, Other)
2. WHEN displaying habits THEN the Habitor System SHALL allow grouping by category with collapsible sections
3. WHEN a user creates a custom category THEN the Habitor System SHALL save the category and make it available for future habits
4. WHEN filtering habits THEN the Habitor System SHALL allow users to filter by one or multiple categories
5. WHEN displaying category statistics THEN the Habitor System SHALL show completion rate per category

### Requirement 11: Habit Data Model Enhancement

**User Story:** As a developer, I want an enhanced data model, so that the app can support all new reminder, priority, and sync features.

#### Acceptance Criteria

1. WHEN storing a habit THEN the Habitor System SHALL include fields for: reminderTime, repeatPattern, repeatDays, isReminderEnabled, firebaseId, lastSyncedAt, streakCount, priority, and category
2. WHEN serializing habit data for Firestore THEN the Habitor System SHALL convert the Habit object to a Map with all required fields
3. WHEN deserializing habit data from Firestore THEN the Habitor System SHALL construct a valid Habit object from the document data
4. WHEN calculating streak count THEN the Habitor System SHALL count consecutive days of completion ending with today or yesterday
5. WHEN migrating from the current database schema THEN the Habitor System SHALL preserve existing habit data and set default values for new fields

