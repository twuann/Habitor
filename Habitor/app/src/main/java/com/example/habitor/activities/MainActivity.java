package com.example.habitor.activities;

import android.os.Bundle;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.habitor.R;
import com.example.habitor.fragments.AddEditHabitBottomSheet;
import com.example.habitor.fragments.HomeFragment;
import com.example.habitor.fragments.ProfileFragment;
import com.example.habitor.fragments.TrashFragment;
import com.example.habitor.sync.SyncManager;
import com.example.habitor.utils.AlarmScheduler;
import com.example.habitor.utils.AuthManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Main activity with bottom navigation bar.
 * 
 * Requirements: 1.1, 1.2, 1.4, 3.1, 3.2
 */
public class MainActivity extends AppCompatActivity
        implements AuthManager.AuthStateListener,
        AuthManager.OnSyncCompleteListener {

    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private FloatingActionButton fabAddHabit;
    private AuthManager authManager;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display (transparent status bar)
        EdgeToEdge.enable(this);
        
        setContentView(R.layout.activity_main);

        // Initialize AuthManager
        authManager = AuthManager.getInstance(this);
        authManager.addAuthStateListener(this);
        authManager.addSyncCompleteListener(this);

        // Initialize SyncManager and register network callback for offline queue processing
        // Requirement 6.3: Process queue when connectivity is restored
        syncManager = new SyncManager(this);
        syncManager.registerNetworkCallback();

        toolbar = findViewById(R.id.toolbar);
        fabAddHabit = findViewById(R.id.fabAddHabit);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // ====== Toolbar ======
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Habitor");
        }

        // ====== Toolbar back button listener ======
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // ====== Setup Bottom Navigation ======
        setupBottomNavigation();

        // ====== FAB click listener ======
        fabAddHabit.setOnClickListener(v -> showAddHabitBottomSheet());

        // ====== Default to Home fragment ======
        if (savedInstanceState == null) {
            navigateToFragment(new HomeFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        // ====== Schedule end-of-day reminder for high priority habits ======
        AlarmScheduler alarmScheduler = new AlarmScheduler(this);
        alarmScheduler.scheduleEndOfDayReminder();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (authManager != null) {
            authManager.removeAuthStateListener(this);
            authManager.removeSyncCompleteListener(this);
        }
        // Unregister network callback to prevent memory leaks
        if (syncManager != null) {
            syncManager.unregisterNetworkCallback();
        }
    }

    /**
     * AuthStateListener callback - called when auth state changes.
     * Requirements: 3.1, 3.2
     */
    @Override
    public void onAuthStateChanged(boolean isSignedIn) {
        // Auth state changes are handled by ProfileFragment
    }

    /**
     * OnSyncCompleteListener callback - refresh data after sync completes.
     * Requirement 6.1: Refresh data after auto-sync on sign in.
     */
    @Override
    public void onSyncComplete(boolean success, String message) {
        runOnUiThread(() -> {
            // Refresh the current fragment if it's HomeFragment
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof HomeFragment) {
                ((HomeFragment) currentFragment).refreshHabits();
            }
        });
    }

    /**
     * Setup bottom navigation with item selection listener.
     * Requirements: 1.2, 1.4
     */
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_trash) {
                selectedFragment = new TrashFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                navigateToFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    /**
     * Navigate to the specified fragment.
     * Requirements: 1.2
     * 
     * @param fragment The fragment to navigate to
     */
    private void navigateToFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Show the AddEditHabitBottomSheet for creating a new habit.
     * Requirements: 3.2
     */
    private void showAddHabitBottomSheet() {
        AddEditHabitBottomSheet bottomSheet = AddEditHabitBottomSheet.newInstance();
        bottomSheet.setOnHabitSavedListener(habit -> {
            // Refresh the current fragment if it's HomeFragment
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof HomeFragment) {
                ((HomeFragment) currentFragment).refreshHabits();
            }
        });
        bottomSheet.show(getSupportFragmentManager(), "AddEditHabitBottomSheet");
    }

    /**
     * Update nav header - kept for compatibility with ProfileFragment.
     * This method is now a no-op since we removed the navigation drawer.
     */
    public void updateNavHeader() {
        // No-op: Navigation drawer has been removed.
        // Profile information is now displayed in ProfileFragment.
    }

    /**
     * Get the BottomNavigationView for external access.
     * 
     * @return The BottomNavigationView instance
     */
    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationView;
    }

    /**
     * Hide bottom navigation bar and FAB for detail screens.
     * Also shows back button in toolbar.
     * Call this when navigating to detail fragments like HabitDetailFragment, SettingsFragment, etc.
     */
    public void hideBottomNavigation() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.GONE);
        }
        if (fabAddHabit != null) {
            fabAddHabit.setVisibility(View.GONE);
        }
        // Show back button in toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    /**
     * Show bottom navigation bar and FAB for main screens.
     * Also hides back button in toolbar.
     * Call this when returning to main fragments like HomeFragment, TrashFragment, ProfileFragment.
     */
    public void showBottomNavigation() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
        if (fabAddHabit != null) {
            fabAddHabit.setVisibility(View.VISIBLE);
        }
        // Hide back button in toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
    }
}
