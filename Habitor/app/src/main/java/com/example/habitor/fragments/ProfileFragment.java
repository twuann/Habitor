package com.example.habitor.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.habitor.R;
import com.example.habitor.activities.MainActivity;
import com.example.habitor.utils.PreferenceHelper;

import java.io.IOException;

/**
 * @deprecated This fragment has been replaced by {@link SettingsFragment} which provides
 * a unified settings experience combining Account, Profile, and Notification settings.
 * This class is kept for reference only and should not be used in new code.
 * 
 * <p>Migration: Use {@link SettingsFragment} instead, which includes all profile
 * editing functionality in the Profile section.</p>
 * 
 * @see SettingsFragment
 * @see <a href=".kiro/specs/drawer-auth-sync/requirements.md">Requirements 4.2</a>
 */
@Deprecated
public class ProfileFragment extends Fragment {

    private ImageView imgProfilePic;
    private EditText edtName, edtAge;
    private RadioGroup genderGroup;
    private Button btnSave, btnChangePic;
    private Uri imageUri;

    // ================================
    // ACTIVITY RESULT API (NEW)
    // ================================
    private ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {

                    imageUri = result.getData().getData();

                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                requireActivity().getContentResolver(), imageUri);

                        imgProfilePic.setImageBitmap(bitmap);

                        // Save immediately
                        PreferenceHelper.saveUserImage(requireContext(), imageUri.toString());

                        // Update drawer header instantly
                        ((MainActivity) requireActivity()).updateNavHeader();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        imgProfilePic = view.findViewById(R.id.imgProfilePic);
        edtName = view.findViewById(R.id.edtName);
        edtAge = view.findViewById(R.id.edtAge);
        genderGroup = view.findViewById(R.id.genderGroup);
        btnSave = view.findViewById(R.id.btnSave);
        btnChangePic = view.findViewById(R.id.btnChangePic);

        loadUserInfo();

        btnChangePic.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveUserInfo());

        return view;
    }

    private void loadUserInfo() {

        // name
        edtName.setText(PreferenceHelper.getUserName(requireContext()));

        // age
        int age = PreferenceHelper.getUserAge(requireContext());
        if (age > 0) edtAge.setText(String.valueOf(age));

        // gender
        String gender = PreferenceHelper.getUserGender(requireContext());
        if (gender.equalsIgnoreCase("Male"))
            genderGroup.check(R.id.rbMale);
        else if (gender.equalsIgnoreCase("Female"))
            genderGroup.check(R.id.rbFemale);

        // profile image
        String imageUriStr = PreferenceHelper.getUserImage(requireContext());
        if (!imageUriStr.isEmpty()) {
            imgProfilePic.setImageURI(Uri.parse(imageUriStr));
        } else {
            imgProfilePic.setImageResource(R.drawable.ic_habitor_placeholder);
        }
    }

    private void saveUserInfo() {

        String name = edtName.getText().toString().trim();
        String ageStr = edtAge.getText().toString().trim();

        if (name.isEmpty() || ageStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = Integer.parseInt(ageStr);

        // prevent crash when none selected
        int selectedId = genderGroup.getCheckedRadioButtonId();
        String gender = "";
        if (selectedId != -1) {
            gender = ((RadioButton) requireView().findViewById(selectedId)).getText().toString();
        }

        // Save basic info
        PreferenceHelper.saveUserInfo(requireContext(), name, age, gender);

        // Save image if chosen before
        if (imageUri != null) {
            PreferenceHelper.saveUserImage(requireContext(), imageUri.toString());
        }

        // Update drawer header
        ((MainActivity) requireActivity()).updateNavHeader();

        Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }
}
