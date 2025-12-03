package com.example.habitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habitor.R;
import com.example.habitor.utils.DeviceIdHelper;
import com.example.habitor.utils.PreferenceHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity for user onboarding flow.
 * Collects user profile information and initializes Firestore user document.
 * 
 * Requirements: 2.2, 2.3
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final String TAG = "OnboardingActivity";
    private static final String COLLECTION_USERS = "users";
    private static final String DOCUMENT_PROFILE = "profile";

    TextInputEditText edtName, edtAge;
    RadioGroup genderGroup;
    Button btnContinue;
    
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // If user has already completed onboarding, go to MainActivity directly
        // Requirement 2.4: Skip onboarding and load home screen directly
        if (PreferenceHelper.isOnboarded(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        edtName = findViewById(R.id.edtName);
        edtAge = findViewById(R.id.edtAge);
        genderGroup = findViewById(R.id.genderGroup);
        btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String ageStr = edtAge.getText().toString().trim();

            if (name.isEmpty() || ageStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedId = genderGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
                return;
            }
            String gender = ((RadioButton) findViewById(selectedId)).getText().toString();

            // Disable button to prevent double-clicks
            btnContinue.setEnabled(false);

            // Complete onboarding with user info
            completeOnboarding(name, age, gender);
        });
    }

    /**
     * Complete the onboarding process.
     * Generates device user ID and initializes Firestore user document.
     * 
     * Requirements: 2.2, 2.3
     *
     * @param name User's name
     * @param age User's age
     * @param gender User's gender
     */
    private void completeOnboarding(String name, int age, String gender) {
        // Requirement 2.2: Generate unique device-based user ID
        String deviceUserId = DeviceIdHelper.getDeviceUserId(this);
        
        // Save device user ID to PreferenceHelper as well for easy access
        PreferenceHelper.saveDeviceUserId(this, deviceUserId);
        
        Log.d(TAG, "Generated device user ID: " + deviceUserId);

        // Save user info to SharedPreferences
        PreferenceHelper.saveUserInfo(this, name, age, gender);

        // Requirement 2.3: Initialize Firestore user document
        initializeFirestoreUserDocument(deviceUserId, name, age, gender);
    }

    /**
     * Initialize the Firestore user document with profile information.
     * 
     * Requirement 2.3: Navigate to home screen and initialize local database
     *
     * @param userId The device user ID
     * @param name User's name
     * @param age User's age
     * @param gender User's gender
     */
    private void initializeFirestoreUserDocument(String userId, String name, int age, String gender) {
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", name);
        profileData.put("age", age);
        profileData.put("gender", gender);
        profileData.put("createdAt", System.currentTimeMillis());
        profileData.put("updatedAt", System.currentTimeMillis());

        firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(DOCUMENT_PROFILE)
                .document("info")
                .set(profileData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore user document initialized successfully");
                    PreferenceHelper.setFirestoreInitialized(this, true);
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to initialize Firestore user document: " + e.getMessage());
                    // Still proceed to main activity even if Firestore fails
                    // The sync will be attempted again later
                    Toast.makeText(this, "Profile saved locally. Cloud sync will retry later.", 
                            Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                });
    }

    /**
     * Navigate to MainActivity after onboarding is complete.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
