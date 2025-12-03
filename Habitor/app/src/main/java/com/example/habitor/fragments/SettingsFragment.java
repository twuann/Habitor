package com.example.habitor.fragments;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.habitor.R;
import com.example.habitor.activities.MainActivity;
import com.example.habitor.sync.SyncManager;
import com.example.habitor.utils.AlarmReceiver;
import com.example.habitor.utils.AuthManager;
import com.example.habitor.utils.PreferenceHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Unified Settings Fragment that combines Account, Profile, and Notification settings.
 * Replaces the old ProfileFragment and NotifSettingsFragment.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
public class SettingsFragment extends Fragment implements AuthManager.AuthStateListener {

    // Account section views
    private TextView tvEmail;
    private TextView tvSyncStatus;
    private TextView tvLastSync;
    private TextView tvSignInPrompt;
    private Button btnSignOut;
    private Button btnSyncNow;
    private Button btnSignIn;
    private LinearLayout layoutSyncStatus;
    private LinearLayout layoutLastSync;

    // Profile section views
    private ImageView imgProfilePic;
    private TextInputEditText edtName;
    private TextInputEditText edtAge;
    private RadioGroup genderGroup;
    private Button btnChangePic;
    private Button btnSaveProfile;
    private Uri imageUri;

    // Notification section views
    private SwitchCompat switchDailyReminder;
    private SwitchCompat switchEndOfDayReminder;
    private LinearLayout layoutReminderTime;
    private TimePicker timePicker;

    // Managers
    private AuthManager authManager;
    private SyncManager syncManager;
    private AlarmManager alarmManager;
    private PendingIntent dailyReminderPendingIntent;

    // Activity result launcher for image picker
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                requireActivity().getContentResolver(), imageUri);
                        imgProfilePic.setImageBitmap(bitmap);
                        
                        // Save immediately
                        PreferenceHelper.saveUserImage(requireContext(), imageUri.toString());
                        
                        // Update drawer header
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).updateNavHeader();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = AuthManager.getInstance(requireContext());
        syncManager = new SyncManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        initViews(view);
        setupListeners();
        loadData();
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        authManager.addAuthStateListener(this);
        updateAccountSection();
        // Hide bottom navigation when viewing settings
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        authManager.removeAuthStateListener(this);
        // Show bottom navigation when leaving settings
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showBottomNavigation();
        }
    }

    /**
     * AuthStateListener callback - update UI when auth state changes.
     */
    @Override
    public void onAuthStateChanged(boolean isSignedIn) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::updateAccountSection);
        }
    }

    /**
     * Initialize all views from the layout.
     */
    private void initViews(View view) {
        // Account section
        tvEmail = view.findViewById(R.id.tvEmail);
        tvSyncStatus = view.findViewById(R.id.tvSyncStatus);
        tvLastSync = view.findViewById(R.id.tvLastSync);
        tvSignInPrompt = view.findViewById(R.id.tvSignInPrompt);
        btnSignOut = view.findViewById(R.id.btnSignOut);
        btnSyncNow = view.findViewById(R.id.btnSyncNow);
        btnSignIn = view.findViewById(R.id.btnSignIn);
        layoutSyncStatus = view.findViewById(R.id.layoutSyncStatus);
        layoutLastSync = view.findViewById(R.id.layoutLastSync);

        // Profile section
        imgProfilePic = view.findViewById(R.id.imgProfilePic);
        edtName = view.findViewById(R.id.edtName);
        edtAge = view.findViewById(R.id.edtAge);
        genderGroup = view.findViewById(R.id.genderGroup);
        btnChangePic = view.findViewById(R.id.btnChangePic);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);

        // Notification section
        switchDailyReminder = view.findViewById(R.id.switchDailyReminder);
        switchEndOfDayReminder = view.findViewById(R.id.switchEndOfDayReminder);
        layoutReminderTime = view.findViewById(R.id.layoutReminderTime);
        timePicker = view.findViewById(R.id.timePicker);

        // Initialize alarm manager
        alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);
        dailyReminderPendingIntent = PendingIntent.getBroadcast(
                requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }


    /**
     * Setup click listeners and change listeners.
     */
    private void setupListeners() {
        // Account section listeners
        btnSignOut.setOnClickListener(v -> handleSignOut());
        btnSyncNow.setOnClickListener(v -> handleSyncNow());
        btnSignIn.setOnClickListener(v -> navigateToAuth());

        // Profile section listeners
        btnChangePic.setOnClickListener(v -> openGallery());
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Notification section listeners
        switchDailyReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutReminderTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                setDailyReminder();
            } else {
                cancelDailyReminder();
            }
        });

        switchEndOfDayReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // End of day reminder is handled by AlarmScheduler in MainActivity
            // Just save the preference
            Toast.makeText(getContext(), 
                    isChecked ? "End of day reminder enabled" : "End of day reminder disabled",
                    Toast.LENGTH_SHORT).show();
        });

        // Time picker change listener
        timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            if (switchDailyReminder.isChecked()) {
                setDailyReminder();
            }
        });
    }

    /**
     * Load all data into views.
     */
    private void loadData() {
        updateAccountSection();
        loadProfileData();
        loadNotificationSettings();
    }

    // ========================================
    // ACCOUNT SECTION (Requirements: 5.1, 5.2, 5.3, 5.4)
    // ========================================

    /**
     * Update the account section based on auth state.
     * Requirements: 5.1, 5.2
     */
    private void updateAccountSection() {
        if (authManager.isSignedIn()) {
            // Show signed-in state
            String email = authManager.getCurrentUserEmail();
            tvEmail.setText(email != null ? email : "Signed In");
            
            // Show sync-related views
            layoutSyncStatus.setVisibility(View.VISIBLE);
            layoutLastSync.setVisibility(View.VISIBLE);
            btnSyncNow.setVisibility(View.VISIBLE);
            btnSignOut.setVisibility(View.VISIBLE);
            tvSignInPrompt.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.GONE);
            
            // Update sync status
            updateSyncStatus();
            
            // Update last sync time
            updateLastSyncTime();
        } else {
            // Show signed-out state
            tvEmail.setText("Not signed in");
            
            // Hide sync-related views
            layoutSyncStatus.setVisibility(View.GONE);
            layoutLastSync.setVisibility(View.GONE);
            btnSyncNow.setVisibility(View.GONE);
            btnSignOut.setVisibility(View.GONE);
            tvSignInPrompt.setVisibility(View.VISIBLE);
            btnSignIn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Navigate to AuthFragment for sign in.
     */
    private void navigateToAuth() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            AuthFragment authFragment = AuthFragment.newInstance();
            authFragment.setOnAuthSuccessListener(() -> {
                mainActivity.updateNavHeader();
            });
            
            mainActivity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, authFragment)
                    .commit();
        }
    }

    /**
     * Update sync status display.
     */
    private void updateSyncStatus() {
        if (syncManager.isOnline()) {
            tvSyncStatus.setText("Connected");
            tvSyncStatus.setTextColor(getResources().getColor(R.color.completed_green, null));
        } else {
            tvSyncStatus.setText("Offline");
            tvSyncStatus.setTextColor(getResources().getColor(R.color.priority_medium, null));
        }
    }

    /**
     * Update last sync time display.
     * Requirements: 5.4
     */
    private void updateLastSyncTime() {
        long lastSyncTime = PreferenceHelper.getLastSyncTime(requireContext());
        if (lastSyncTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            tvLastSync.setText(sdf.format(new Date(lastSyncTime)));
        } else {
            tvLastSync.setText("Never");
        }
    }

    /**
     * Handle sign out button click.
     * Requirements: 5.3
     */
    private void handleSignOut() {
        authManager.signOut();
        PreferenceHelper.clearAuthState(requireContext());
        
        Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
        
        // Update drawer header
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateNavHeader();
        }
        
        // Update account section
        updateAccountSection();
    }

    /**
     * Handle sync now button click.
     * Requirements: 5.5
     */
    private void handleSyncNow() {
        if (!syncManager.isOnline()) {
            Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSyncNow.setEnabled(false);
        btnSyncNow.setText("Syncing...");

        syncManager.syncOnAppLaunch((success, message) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    btnSyncNow.setEnabled(true);
                    btnSyncNow.setText("Sync Now");
                    
                    if (success) {
                        PreferenceHelper.saveLastSyncTime(requireContext(), System.currentTimeMillis());
                        updateLastSyncTime();
                        Toast.makeText(getContext(), "Sync completed", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Sync failed: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


    // ========================================
    // PROFILE SECTION (Migrated from ProfileFragment)
    // ========================================

    /**
     * Load profile data from preferences.
     */
    private void loadProfileData() {
        // Load name
        edtName.setText(PreferenceHelper.getUserName(requireContext()));

        // Load age
        int age = PreferenceHelper.getUserAge(requireContext());
        if (age > 0) {
            edtAge.setText(String.valueOf(age));
        }

        // Load gender
        String gender = PreferenceHelper.getUserGender(requireContext());
        if ("Male".equalsIgnoreCase(gender)) {
            genderGroup.check(R.id.rbMale);
        } else if ("Female".equalsIgnoreCase(gender)) {
            genderGroup.check(R.id.rbFemale);
        }

        // Load profile image
        String imageUriStr = PreferenceHelper.getUserImage(requireContext());
        if (imageUriStr != null && !imageUriStr.isEmpty()) {
            imgProfilePic.setImageURI(Uri.parse(imageUriStr));
        } else {
            imgProfilePic.setImageResource(R.drawable.ic_habitor_placeholder);
        }
    }

    /**
     * Save profile data to preferences.
     */
    private void saveProfile() {
        String name = edtName.getText() != null ? edtName.getText().toString().trim() : "";
        String ageStr = edtAge.getText() != null ? edtAge.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = 0;
        if (!ageStr.isEmpty()) {
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid age", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Get selected gender
        String gender = "";
        int selectedId = genderGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadio = requireView().findViewById(selectedId);
            gender = selectedRadio.getText().toString();
        }

        // Save to preferences
        PreferenceHelper.saveUserInfo(requireContext(), name, age, gender);

        // Save image if selected
        if (imageUri != null) {
            PreferenceHelper.saveUserImage(requireContext(), imageUri.toString());
        }

        // Update drawer header
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateNavHeader();
        }

        Toast.makeText(getContext(), "Profile saved!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Open gallery to pick profile image.
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    // ========================================
    // NOTIFICATION SECTION (Migrated from NotifSettingsFragment)
    // ========================================

    /**
     * Load notification settings.
     */
    private void loadNotificationSettings() {
        // For now, switches start unchecked
        // In a full implementation, you would load these from preferences
        switchDailyReminder.setChecked(false);
        switchEndOfDayReminder.setChecked(false);
        layoutReminderTime.setVisibility(View.GONE);
    }

    /**
     * Set daily reminder alarm.
     */
    private void setDailyReminder() {
        Calendar calendar = Calendar.getInstance();
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If the time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                dailyReminderPendingIntent
        );

        Toast.makeText(getContext(), 
                "Daily reminder set for " + hour + ":" + String.format(Locale.getDefault(), "%02d", minute),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Cancel daily reminder alarm.
     */
    private void cancelDailyReminder() {
        if (alarmManager != null && dailyReminderPendingIntent != null) {
            alarmManager.cancel(dailyReminderPendingIntent);
            Toast.makeText(getContext(), "Daily reminder disabled", Toast.LENGTH_SHORT).show();
        }
    }
}
