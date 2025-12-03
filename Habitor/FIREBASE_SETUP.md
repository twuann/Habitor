# Firebase Setup Guide for Habitor

This guide provides step-by-step instructions for integrating Firebase services into the Habitor Android app.

## Prerequisites

- Android Studio installed
- Google account for Firebase Console access
- Habitor project cloned and buildable

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"**
3. Enter project name: `Habitor`
4. (Optional) Disable Google Analytics if not needed
5. Click **"Create project"**

<!-- Screenshot placeholder: Firebase Console - Create Project -->

## Step 2: Add Android App to Firebase

1. In Firebase Console, click the **Android icon** to add an Android app
2. Enter the following details:
   - **Package name**: `com.example.habitor`
   - **App nickname**: `Habitor Android`
   - **Debug signing certificate SHA-1**: (Optional, for Google Sign-In)
3. Click **"Register app"**

<!-- Screenshot placeholder: Firebase Console - Register Android App -->

## Step 3: Download Configuration File

1. Download the `google-services.json` file
2. Place the file in the `Habitor/app/` directory
3. Verify the file location:
   ```
   Habitor/
   ├── app/
   │   ├── google-services.json  <-- Place here
   │   ├── build.gradle
   │   └── src/
   └── build.gradle
   ```

<!-- Screenshot placeholder: Project structure with google-services.json -->

## Step 4: Verify Gradle Configuration

The gradle files have been pre-configured. Verify the following:

### Project-level build.gradle (`Habitor/build.gradle`)
```groovy
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
}
```

### App-level build.gradle (`Habitor/app/build.gradle`)
```groovy
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

dependencies {
    // Firebase BoM (Bill of Materials)
    implementation platform(libs.firebase.bom)
    implementation libs.firebase.firestore
    implementation libs.firebase.messaging
}
```


### Version Catalog (`Habitor/gradle/libs.versions.toml`)
```toml
[versions]
firebaseBom = "32.7.0"
googleServices = "4.4.0"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }

[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

## Step 5: Setup Firestore Database

1. In Firebase Console, navigate to **Build → Firestore Database**
2. Click **"Create database"**
3. Select **"Start in test mode"** (for development)
4. Choose the region closest to your users
5. Click **"Enable"**

<!-- Screenshot placeholder: Firestore Database creation -->

### Database Schema

The Habitor app uses the following Firestore structure:

```
users/{userId}/
├── profile/
│   ├── name: string
│   ├── age: number
│   └── gender: string
├── habits/{habitId}/
│   ├── name: string
│   ├── note: string
│   ├── isDeleted: boolean
│   ├── reminderTime: string
│   ├── isReminderEnabled: boolean
│   ├── repeatPattern: string
│   ├── repeatDays: array
│   ├── customIntervalDays: number
│   ├── priority: string
│   ├── category: string
│   ├── streakCount: number
│   ├── createdAt: timestamp
│   └── updatedAt: timestamp
├── history/{historyId}/
│   ├── habitId: string
│   ├── habitName: string
│   ├── date: string
│   ├── completed: boolean
│   └── completedAt: timestamp
└── categories/{categoryId}/
    ├── name: string
    ├── color: string
    └── isDefault: boolean
```

### Security Rules (Production)

For production deployment, update Firestore security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own data
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth == null || request.auth.uid == userId;
    }
  }
}
```

## Step 6: Setup Firebase Cloud Messaging (FCM)

1. In Firebase Console, navigate to **Build → Cloud Messaging**
2. FCM is automatically enabled for your project
3. For server-side notifications (optional):
   - Go to **Project Settings → Cloud Messaging**
   - Note the **Server Key** for backend integration

<!-- Screenshot placeholder: FCM configuration -->

## Step 7: Sync and Build

1. In Android Studio, click **"Sync Now"** when prompted
2. Build the project: **Build → Make Project**
3. Verify no build errors related to Firebase

## Troubleshooting

### Common Issues

1. **google-services.json not found**
   - Ensure the file is in `Habitor/app/` directory
   - Check the package name matches `com.example.habitor`

2. **Gradle sync failed**
   - Verify internet connection
   - Check gradle version compatibility
   - Try **File → Invalidate Caches / Restart**

3. **Firebase initialization error**
   - Verify google-services.json is valid
   - Check Firebase project is properly configured

### Verification

To verify Firebase is properly configured:

```java
// In any Activity or Application class
FirebaseFirestore db = FirebaseFirestore.getInstance();
Log.d("Firebase", "Firestore instance: " + db);
```

## Next Steps

After completing Firebase setup:
1. Implement user authentication (device-based ID)
2. Set up data synchronization with Firestore
3. Configure push notifications with FCM
