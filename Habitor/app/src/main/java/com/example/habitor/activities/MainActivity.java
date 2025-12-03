package com.example.habitor.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.habitor.R;
import com.example.habitor.fragments.AddEditHabitBottomSheet;
import com.example.habitor.fragments.AuthFragment;
import com.example.habitor.fragments.HomeFragment;
import com.example.habitor.fragments.SettingsFragment;
import com.example.habitor.fragments.TrashFragment;
import com.example.habitor.sync.SyncManager;
import com.example.habitor.utils.AlarmScheduler;
import com.example.habitor.utils.AuthManager;
import com.example.habitor.utils.PreferenceHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

/**
 * Main activity with navigation drawer.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 6.1
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        AuthManager.AuthStateListener,
        AuthManager.OnSyncCompleteListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
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

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        fabAddHabit = findViewById(R.id.fabAddHabit);

        // ====== Toolbar ======
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Habitor");
        }

        // ====== Toggle icon (menu icon) ======
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.app_name,
                R.string.app_name
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Listener chọn menu
        navigationView.setNavigationItemSelectedListener(this);

        // Setup nav header click listener
        setupNavHeaderClickListener();

        // Apply window insets to nav header for status bar
        applyNavHeaderInsets();

        // Cập nhật header lần đầu
        updateNavHeader();

        // ====== FAB click listener ======
        fabAddHabit.setOnClickListener(v -> showAddHabitBottomSheet());

        // ====== Mặc định mở Home ======
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }

        // ====== Schedule end-of-day reminder for high priority habits ======
        AlarmScheduler alarmScheduler = new AlarmScheduler(this);
        alarmScheduler.scheduleEndOfDayReminder();

        // ====== Xử lý nút Back ======
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                }
            }
        });
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
     * AuthStateListener callback - update nav header when auth state changes.
     * Requirements: 3.1, 3.2
     */
    @Override
    public void onAuthStateChanged(boolean isSignedIn) {
        runOnUiThread(this::updateNavHeader);
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
            // Update nav header to reflect sync status
            updateNavHeader();
        });
    }


    /**
     * Apply window insets to nav header for proper status bar padding.
     */
    private void applyNavHeaderInsets() {
        View header = navigationView.getHeaderView(0);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    insets.top + 16, // Add status bar height + extra padding
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return WindowInsetsCompat.CONSUMED;
        });
        // Request insets to be applied
        ViewCompat.requestApplyInsets(header);
    }

    /**
     * Setup click listener for nav header.
     * Requirements: 3.3, 3.4
     */
    private void setupNavHeaderClickListener() {
        View header = navigationView.getHeaderView(0);
        LinearLayout navHeaderLayout = header.findViewById(R.id.navHeaderLayout);
        
        if (navHeaderLayout != null) {
            navHeaderLayout.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                
                // Navigate based on auth state
                if (authManager.isSignedIn()) {
                    // Navigate to Settings when signed in (Requirements: 3.4)
                    navigateToSettings();
                } else {
                    // Navigate to Auth when not signed in (Requirements: 3.3)
                    navigateToAuth();
                }
            });
        }
    }

    /**
     * Navigate to AuthFragment.
     * Requirements: 3.3
     */
    private void navigateToAuth() {
        AuthFragment authFragment = AuthFragment.newInstance();
        authFragment.setOnAuthSuccessListener(() -> {
            updateNavHeader();
            navigationView.setCheckedItem(R.id.nav_home);
        });
        
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, authFragment)
                .commit();
        
        // Uncheck all menu items when showing auth
        navigationView.setCheckedItem(View.NO_ID);
    }

    /**
     * Navigate to SettingsFragment.
     * Requirements: 3.4, 5.1
     */
    private void navigateToSettings() {
        SettingsFragment settingsFragment = SettingsFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, settingsFragment)
                .commit();
        navigationView.setCheckedItem(R.id.nav_settings);
    }

    /**
     * Show the AddEditHabitBottomSheet for creating a new habit.
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
     * Update nav header to show auth state.
     * Requirements: 3.1, 3.2
     * 
     * - When not signed in: display "Tap to sign in" with default avatar
     * - When signed in: display user's email and sync status indicator
     */
    public void updateNavHeader() {
        View header = navigationView.getHeaderView(0);

        ImageView imgProfile = header.findViewById(R.id.imgProfilePic);
        TextView tvName = header.findViewById(R.id.tvUserName);
        TextView tvSyncStatus = header.findViewById(R.id.tvSyncStatus);

        if (authManager.isSignedIn()) {
            // Signed in - show email (Requirements: 3.2)
            String email = authManager.getCurrentUserEmail();
            tvName.setText(email != null ? email : "Signed In");
            
            // Show sync status
            if (tvSyncStatus != null) {
                tvSyncStatus.setText("Syncing enabled");
                tvSyncStatus.setVisibility(View.VISIBLE);
            }
            
            // Try to load user profile image, fallback to default
            String imageUri = PreferenceHelper.getUserImage(this);
            if (imageUri == null || imageUri.isEmpty()) {
                imgProfile.setImageResource(R.drawable.ic_habitor_placeholder);
            } else {
                imgProfile.setImageURI(Uri.parse(imageUri));
            }
        } else {
            // Not signed in - show "Tap to sign in" (Requirements: 3.1)
            tvName.setText("Tap to sign in");
            
            // Hide sync status
            if (tvSyncStatus != null) {
                tvSyncStatus.setVisibility(View.GONE);
            }
            
            // Show default avatar
            imgProfile.setImageResource(R.drawable.ic_habitor_placeholder);
        }
    }

    /**
     * Handle menu item selection.
     * Requirements: 4.1, 4.2, 4.3
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {

        Fragment selectedFragment = null;

        int id = item.getItemId();
        if (id == R.id.nav_home) {
            selectedFragment = new HomeFragment();
        } else if (id == R.id.nav_trash) {
            selectedFragment = new TrashFragment();
        } else if (id == R.id.nav_settings) {
            // Navigate to Settings (Requirements: 4.1, 4.3)
            navigateToSettings();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (selectedFragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
