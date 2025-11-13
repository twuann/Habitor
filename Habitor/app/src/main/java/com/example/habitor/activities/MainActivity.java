package com.example.habitor.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.habitor.R;
import com.example.habitor.fragments.CalendarFragment;
import com.example.habitor.fragments.HomeFragment;
import com.example.habitor.fragments.NotifSettingsFragment;
import com.example.habitor.fragments.ProfileFragment;
import com.example.habitor.fragments.SearchFragment;
import com.example.habitor.fragments.TrashFragment;
import com.example.habitor.utils.PreferenceHelper;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

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

        // Cập nhật header lần đầu
        updateNavHeader();

        // ====== Mặc định mở Home ======
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }

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

    // ====== Hàm UPDATE header ======
    public void updateNavHeader() {
        View header = navigationView.getHeaderView(0);

        ImageView imgProfile = header.findViewById(R.id.imgProfilePic);
        TextView tvName = header.findViewById(R.id.tvUserName);

        // name
        String name = PreferenceHelper.getUserName(this);
        tvName.setText(name);

        // image
        String imageUri = PreferenceHelper.getUserImage(this);

        if (imageUri == null || imageUri.isEmpty()) {
            imgProfile.setImageResource(R.drawable.ic_habitor_placeholder);
        } else {
            imgProfile.setImageURI(Uri.parse(imageUri));
        }
    }

    // ====== Xử lý chọn menu ======
    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {

        Fragment selectedFragment = null;

        int id = item.getItemId();
        if (id == R.id.nav_home) {
            selectedFragment = new HomeFragment();
        } else if (id == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
        } else if (id == R.id.nav_search) {
            selectedFragment = new SearchFragment();
        } else if (id == R.id.nav_trash) {
            selectedFragment = new TrashFragment();
        } else if (id == R.id.nav_calendar) {
            selectedFragment = new CalendarFragment();
        } else if (id == R.id.nav_notif) {
            selectedFragment = new NotifSettingsFragment();
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
