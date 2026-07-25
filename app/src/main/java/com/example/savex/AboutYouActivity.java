package com.example.savex;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.savex.databinding.ActivityAboutYouBinding;
import com.example.savex.model.OnboardingSession;

public class AboutYouActivity extends AppCompatActivity {

    private ActivityAboutYouBinding binding;

    private int heightInches = 68; // starting default: 5'8"
    private double weightKg = 70;  // starting default: 70 kg

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutYouBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupStepIndicator();
        setupHeightStepper();
        setupWeightStepper();
        setupNextButton();
    }

    private void setupStepIndicator() {
        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.gw_step_active);
        binding.stepIndicator.step1Dot.setBackgroundColor(activeColor);
    }

    private void setupHeightStepper() {
        updateHeightDisplay();

        binding.btnHeightMinus.setOnClickListener(v -> {
            if (heightInches > 48) { // don't go below 4'0"
                heightInches--;
                updateHeightDisplay();
            }
        });

        binding.btnHeightPlus.setOnClickListener(v -> {
            if (heightInches < 84) { // cap at 7'0"
                heightInches++;
                updateHeightDisplay();
            }
        });
    }

    private void updateHeightDisplay() {
        int feet = heightInches / 12;
        int inches = heightInches % 12;
        binding.textHeightValue.setText(feet + "'" + inches + "\"");
    }

    private void setupWeightStepper() {
        updateWeightDisplay();

        binding.btnWeightMinus.setOnClickListener(v -> {
            if (weightKg > 30) {
                weightKg -= 1;
                updateWeightDisplay();
            }
        });

        binding.btnWeightPlus.setOnClickListener(v -> {
            if (weightKg < 200) {
                weightKg += 1;
                updateWeightDisplay();
            }
        });
    }

    private void updateWeightDisplay() {
        binding.textWeightValue.setText(((int) weightKg) + " kg");
    }

    private void setupNextButton() {
        binding.btnNextAboutYou.setOnClickListener(v -> collectAndProceed());
    }

    private void collectAndProceed() {
        OnboardingSession session = OnboardingSession.getInstance();

        String selectedGender = binding.btnGenderMale.isChecked()
                ? getString(R.string.gender_male)
                : getString(R.string.gender_female);
        session.setGender(selectedGender);

        double heightCm = heightInches * 2.54;
        session.setHeightCm(heightCm);
        session.setWeightKg(weightKg);

        startActivity(new Intent(this, YourGoalActivity.class));
    }
}