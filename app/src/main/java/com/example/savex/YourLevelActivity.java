package com.example.savex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.savex.databinding.ActivityYourLevelBinding;
import com.example.savex.model.OnboardingSession;

public class YourLevelActivity extends AppCompatActivity {

    private ActivityYourLevelBinding binding;

    private static final String[] EXERCISES = {"Bench Press", "Squat", "Deadlift", "Overhead Press"};

    private double weightLifted = 30; // kg, matches the mockup's default
    private int repsTillTired = 5;
    private double estimated1RM = 35;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityYourLevelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupStepIndicator();
        setupExerciseDropdown();
        setupSteppers();
        setupBeginnerLink();
        setupBackButton();
        setupNextButton();

        calculateAndDisplay1RM(); // show the correct starting value immediately
    }

    private void setupStepIndicator() {
        int activeColor = ContextCompat.getColor(this, R.color.gw_step_active);
        binding.stepIndicator.step1Dot.setBackgroundColor(activeColor);
        binding.stepIndicator.step2Dot.setBackgroundColor(activeColor);
        binding.stepIndicator.step3Dot.setBackgroundColor(activeColor);
    }

    private void setupExerciseDropdown() {
        binding.dropdownExercise.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, EXERCISES));
        binding.dropdownExercise.setText(EXERCISES[0], false);
    }

    private void setupSteppers() {
        binding.btnLevelWeightMinus.setOnClickListener(v -> {
            if (weightLifted > 2.5) {
                weightLifted -= 2.5;
                calculateAndDisplay1RM();
            }
        });

        binding.btnLevelWeightPlus.setOnClickListener(v -> {
            weightLifted += 2.5;
            calculateAndDisplay1RM();
        });

        binding.btnRepsMinus.setOnClickListener(v -> {
            if (repsTillTired > 1) {
                repsTillTired--;
                calculateAndDisplay1RM();
            }
        });

        binding.btnRepsPlus.setOnClickListener(v -> {
            if (repsTillTired < 20) { // beyond ~20 reps the 1RM formula becomes unreliable
                repsTillTired++;
                calculateAndDisplay1RM();
            }
        });
    }

    /**
     * The live-updating 1RM estimate — this is the exact "no submit button,
     * recalculates instantly" behavior from the reference video.
     * Uses the Epley formula: 1RM = weight × (1 + reps / 30)
     */
    private void calculateAndDisplay1RM() {
        estimated1RM = weightLifted * (1 + (repsTillTired / 30.0));

        binding.textLevelWeightValue.setText(formatWeight(weightLifted) + " kg");
        binding.textRepsValue.setText(String.valueOf(repsTillTired));
        binding.text1RMResult.setText(formatWeight(estimated1RM) + " kg");
    }

    /** Displays whole numbers cleanly (30, not 30.0) but keeps .5 precision when it matters. */
    private String formatWeight(double value) {
        double rounded = Math.round(value * 2) / 2.0;
        if (rounded == Math.floor(value)) {
            return String.valueOf((int) rounded);
        }
        return String.valueOf(rounded);
    }

    private void setupBeginnerLink() {
        binding.textBeginnerLink.setOnClickListener(v -> {
            // Beginner fallback: skip the estimate entirely, use a conservative
            // default so plan generation still has something reasonable to work with.
            weightLifted = 0;
            repsTillTired = 0;
            estimated1RM = 0;
            proceedToNextScreen();
        });
    }

    private void setupBackButton() {
        binding.btnBackYourLevel.setOnClickListener(v -> finish());
    }

    private void setupNextButton() {
        binding.btnNextYourLevel.setOnClickListener(v -> proceedToNextScreen());
    }

    private void proceedToNextScreen() {
        OnboardingSession session = OnboardingSession.getInstance();
        session.setBenchmarkExercise(binding.dropdownExercise.getText().toString());
        session.setWeightLifted(weightLifted);
        session.setRepsTillTired(repsTillTired);
        session.setEstimated1RM(estimated1RM);

        startActivity(new Intent(this, YourSetupActivity.class));
    }
}