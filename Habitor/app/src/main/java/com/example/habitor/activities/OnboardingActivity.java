package com.example.habitor.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import com.example.habitor.R;
import com.example.habitor.utils.PreferenceHelper;

public class OnboardingActivity extends AppCompatActivity {

    EditText edtName, edtAge;
    RadioGroup genderGroup;
    Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Nếu user đã nhập info rồi → qua MainActivity luôn
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
            String name = edtName.getText().toString();
            String ageStr = edtAge.getText().toString();

            if (name.isEmpty() || ageStr.isEmpty()) return;

            int age = Integer.parseInt(ageStr);
            int selectedId = genderGroup.getCheckedRadioButtonId();
            String gender = ((RadioButton) findViewById(selectedId)).getText().toString();

            PreferenceHelper.saveUserInfo(this, name, age, gender);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
