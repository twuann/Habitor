package com.example.habitor.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.habitor.R;
import com.example.habitor.activities.MainActivity;
import com.example.habitor.utils.AuthManager;
import com.example.habitor.utils.PreferenceHelper;

/**
 * ProfileFragment displays user information and provides access to Settings.
 * Shows different UI based on authentication state:
 * - Signed in: displays user email, profile image, and Settings option
 * - Not signed in: displays sign-in prompt and Settings option
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4
 */
public class ProfileFragment extends Fragment implements AuthManager.AuthStateListener {

    // User info section views
    private LinearLayout layoutUserInfo;
    private ImageView imgUserAvatar;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private CardView cardSettings;

    // Sign-in prompt section views
    private LinearLayout layoutSignInPrompt;
    private Button btnSignIn;
    private CardView cardSettingsNotSignedIn;

    // Managers
    private AuthManager authManager;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance of ProfileFragment.
     * 
     * @return A new instance of ProfileFragment
     */
    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = AuthManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_new, container, false);
        
        initViews(view);
        setupListeners();
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        authManager.addAuthStateListener(this);
        updateUIForAuthState(authManager.isSignedIn());
    }

    @Override
    public void onPause() {
        super.onPause();
        authManager.removeAuthStateListener(this);
    }

    /**
     * AuthStateListener callback - update UI when auth state changes.
     */
    @Override
    public void onAuthStateChanged(boolean isSignedIn) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> updateUIForAuthState(isSignedIn));
        }
    }

    /**
     * Initialize all views from the layout.
     */
    private void initViews(View view) {
        // User info section
        layoutUserInfo = view.findViewById(R.id.layoutUserInfo);
        imgUserAvatar = view.findViewById(R.id.imgUserAvatar);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        cardSettings = view.findViewById(R.id.cardSettings);

        // Sign-in prompt section
        layoutSignInPrompt = view.findViewById(R.id.layoutSignInPrompt);
        btnSignIn = view.findViewById(R.id.btnSignIn);
        cardSettingsNotSignedIn = view.findViewById(R.id.cardSettingsNotSignedIn);
    }

    /**
     * Setup click listeners for interactive elements.
     */
    private void setupListeners() {
        // Settings navigation (signed in)
        cardSettings.setOnClickListener(v -> navigateToSettings());
        
        // Settings navigation (not signed in)
        cardSettingsNotSignedIn.setOnClickListener(v -> navigateToSettings());
        
        // Sign in button
        btnSignIn.setOnClickListener(v -> navigateToAuth());
    }

    /**
     * Update UI based on authentication state.
     * Requirements: 4.3, 4.4
     * 
     * @param isSignedIn true if user is signed in, false otherwise
     */
    private void updateUIForAuthState(boolean isSignedIn) {
        if (isSignedIn) {
            displayUserInfo();
        } else {
            displaySignInPrompt();
        }
    }

    /**
     * Display user information when signed in.
     * Requirements: 4.1, 4.3
     * 
     * Shows:
     * - User profile image (from preferences or default)
     * - User name (from preferences)
     * - User email (from Firebase Auth)
     * - Settings menu option
     */
    private void displayUserInfo() {
        // Show user info section, hide sign-in prompt
        layoutUserInfo.setVisibility(View.VISIBLE);
        layoutSignInPrompt.setVisibility(View.GONE);

        // Load user email from Firebase Auth
        String email = authManager.getCurrentUserEmail();
        tvUserEmail.setText(email != null ? email : "");

        // Load user name from preferences
        String userName = PreferenceHelper.getUserName(requireContext());
        if (userName != null && !userName.isEmpty()) {
            tvUserName.setText(userName);
        } else {
            // Use email prefix as fallback name
            if (email != null && email.contains("@")) {
                tvUserName.setText(email.substring(0, email.indexOf("@")));
            } else {
                tvUserName.setText("User");
            }
        }

        // Load profile image from preferences
        String imageUriStr = PreferenceHelper.getUserImage(requireContext());
        if (imageUriStr != null && !imageUriStr.isEmpty()) {
            try {
                imgUserAvatar.setImageURI(Uri.parse(imageUriStr));
            } catch (Exception e) {
                // Fallback to placeholder if image loading fails
                imgUserAvatar.setImageResource(R.drawable.ic_habitor_placeholder);
            }
        } else {
            imgUserAvatar.setImageResource(R.drawable.ic_habitor_placeholder);
        }
    }

    /**
     * Display sign-in prompt when not signed in.
     * Requirements: 4.4
     * 
     * Shows:
     * - Welcome message
     * - Sign-in button
     * - Settings menu option (still accessible)
     */
    private void displaySignInPrompt() {
        // Hide user info section, show sign-in prompt
        layoutUserInfo.setVisibility(View.GONE);
        layoutSignInPrompt.setVisibility(View.VISIBLE);
    }

    /**
     * Navigate to SettingsFragment.
     * Requirements: 4.2
     */
    private void navigateToSettings() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            SettingsFragment settingsFragment = SettingsFragment.newInstance();
            
            mainActivity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, settingsFragment)
                    .addToBackStack(null)
                    .commit();
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
                // Refresh profile after successful auth
                updateUIForAuthState(true);
                mainActivity.updateNavHeader();
            });
            
            mainActivity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, authFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
