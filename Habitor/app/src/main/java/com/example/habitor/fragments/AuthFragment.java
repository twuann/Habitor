package com.example.habitor.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.habitor.R;
import com.example.habitor.utils.AuthManager;
import com.example.habitor.utils.MergeDialogHelper;
import com.example.habitor.utils.PreferenceHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * AuthFragment handles user authentication (sign in and sign up).
 * Provides toggle between sign in and sign up modes.
 * 
 * Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 7.3
 */
public class AuthFragment extends Fragment {

    private static final String TAG = "AuthFragment";

    // UI Elements
    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText edtEmail;
    private TextInputEditText edtPassword;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;
    private TextView tvError;
    private TextView tvTogglePrompt;
    private TextView btnToggleMode;
    private TextView btnSkip;

    // State
    private boolean isSignUpMode = false;
    private AuthManager authManager;
    private MergeDialogHelper mergeDialogHelper;
    private AuthManager.OnMergeNeededListener mergeNeededListener;

    // Listener for auth success
    private OnAuthSuccessListener authSuccessListener;

    public interface OnAuthSuccessListener {
        void onAuthSuccess();
    }

    public static AuthFragment newInstance() {
        return new AuthFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = AuthManager.getInstance(requireContext());
        mergeDialogHelper = new MergeDialogHelper(requireContext());
        setupMergeListener();
    }

    /**
     * Set up listener for merge dialog requests.
     * Requirement 7.3: Show merge dialog when signing in with existing local data.
     */
    private void setupMergeListener() {
        mergeNeededListener = new AuthManager.OnMergeNeededListener() {
            @Override
            public void onMergeNeeded(String userId, int localCount, int cloudCount) {
                if (getActivity() == null || !isAdded()) return;
                
                getActivity().runOnUiThread(() -> {
                    Log.d(TAG, "Showing merge dialog: local=" + localCount + ", cloud=" + cloudCount);
                    mergeDialogHelper.showMergeDialog(
                            requireActivity(),
                            userId,
                            localCount,
                            cloudCount,
                            (success, message) -> {
                                Log.d(TAG, "Merge completed: success=" + success + ", message=" + message);
                                // After merge, perform auto-sync
                                authManager.performAutoSync();
                            }
                    );
                });
            }

            @Override
            public void onMergeNotNeeded() {
                Log.d(TAG, "No merge needed");
                // Auto-sync will be triggered automatically
            }
        };
        authManager.addMergeNeededListener(mergeNeededListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove merge listener to prevent memory leaks
        if (mergeNeededListener != null) {
            authManager.removeMergeNeededListener(mergeNeededListener);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auth, container, false);
        initViews(view);
        setupListeners();
        updateUI();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Hide bottom navigation when viewing auth screen
        if (getActivity() instanceof com.example.habitor.activities.MainActivity) {
            ((com.example.habitor.activities.MainActivity) getActivity()).hideBottomNavigation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Show bottom navigation when leaving auth screen
        if (getActivity() instanceof com.example.habitor.activities.MainActivity) {
            ((com.example.habitor.activities.MainActivity) getActivity()).showBottomNavigation();
        }
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tvTitle);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        edtEmail = view.findViewById(R.id.edtEmail);
        edtPassword = view.findViewById(R.id.edtPassword);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        progressBar = view.findViewById(R.id.progressBar);
        tvError = view.findViewById(R.id.tvError);
        tvTogglePrompt = view.findViewById(R.id.tvTogglePrompt);
        btnToggleMode = view.findViewById(R.id.btnToggleMode);
        btnSkip = view.findViewById(R.id.btnSkip);
    }

    private void setupListeners() {
        // Submit button click
        btnSubmit.setOnClickListener(v -> handleSubmit());

        // Toggle mode click
        btnToggleMode.setOnClickListener(v -> toggleMode());

        // Skip button click - navigate to home without signing in
        btnSkip.setOnClickListener(v -> navigateToHome());

        // Clear error when user starts typing
        TextWatcher errorClearWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hideError();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        edtEmail.addTextChangedListener(errorClearWatcher);
        edtPassword.addTextChangedListener(errorClearWatcher);
    }

    /**
     * Toggle between sign in and sign up modes.
     * Requirements: 1.1, 2.1
     */
    private void toggleMode() {
        isSignUpMode = !isSignUpMode;
        updateUI();
        clearInputs();
        hideError();
    }

    /**
     * Update UI based on current mode (sign in or sign up).
     */
    private void updateUI() {
        if (isSignUpMode) {
            tvTitle.setText(R.string.sign_up);
            tvSubtitle.setText(R.string.auth_subtitle_sign_up);
            btnSubmit.setText(R.string.sign_up);
            tvTogglePrompt.setText(R.string.have_account_prompt);
            btnToggleMode.setText(R.string.sign_in);
        } else {
            tvTitle.setText(R.string.sign_in);
            tvSubtitle.setText(R.string.auth_subtitle_sign_in);
            btnSubmit.setText(R.string.sign_in);
            tvTogglePrompt.setText(R.string.no_account_prompt);
            btnToggleMode.setText(R.string.sign_up);
        }
    }

    /**
     * Handle submit button click - either sign in or sign up.
     * Requirements: 1.2, 1.3, 2.2
     */
    private void handleSubmit() {
        String email = getEmailInput();
        String password = getPasswordInput();

        // Validate inputs
        if (!validateInputs(email, password)) {
            return;
        }

        // Show loading state
        setLoadingState(true);
        hideError();

        // Create callback for auth operations
        AuthManager.AuthCallback callback = new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    setLoadingState(false);
                    
                    // Save auth state to preferences
                    String userId = authManager.getCurrentUserId();
                    String userEmail = authManager.getCurrentUserEmail();
                    PreferenceHelper.saveAuthState(requireContext(), userId, userEmail);
                    
                    // Navigate to home
                    navigateToHome();
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    setLoadingState(false);
                    showError(errorMessage);
                });
            }
        };

        // Perform auth operation
        if (isSignUpMode) {
            authManager.signUp(email, password, callback);
        } else {
            authManager.signIn(email, password, callback);
        }
    }

    /**
     * Validate email and password inputs.
     * Requirements: 1.4, 1.6
     */
    private boolean validateInputs(String email, String password) {
        // Validate email
        if (!AuthManager.isValidEmail(email)) {
            showError(AuthManager.ERROR_INVALID_EMAIL);
            tilEmail.setError(" ");
            return false;
        }
        tilEmail.setError(null);

        // Validate password
        if (!AuthManager.isValidPassword(password)) {
            showError(AuthManager.ERROR_WEAK_PASSWORD);
            tilPassword.setError(" ");
            return false;
        }
        tilPassword.setError(null);

        return true;
    }

    private String getEmailInput() {
        return edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
    }

    private String getPasswordInput() {
        return edtPassword.getText() != null ? edtPassword.getText().toString() : "";
    }

    private void clearInputs() {
        edtEmail.setText("");
        edtPassword.setText("");
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!isLoading);
        edtEmail.setEnabled(!isLoading);
        edtPassword.setEnabled(!isLoading);
        btnToggleMode.setEnabled(!isLoading);
        btnSkip.setEnabled(!isLoading);
    }

    /**
     * Navigate to home screen after successful auth or skip.
     * Requirements: 1.3, 2.2
     */
    private void navigateToHome() {
        // Notify listener if set
        if (authSuccessListener != null) {
            authSuccessListener.onAuthSuccess();
        }

        // Navigate to HomeFragment
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }

    public void setOnAuthSuccessListener(OnAuthSuccessListener listener) {
        this.authSuccessListener = listener;
    }
}
