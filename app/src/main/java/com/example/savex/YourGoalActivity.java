package com.example.savex;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.savex.databinding.ActivityYourGoalBinding;
import com.example.savex.model.OnboardingSession;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class YourGoalActivity extends AppCompatActivity {

    private ActivityYourGoalBinding binding;
    private MaterialCardView selectedGoalCard;
    private String selectedGoalText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityYourGoalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupStepIndicator();
        setupGoalCards();
        setupBackButton();
        setupNextButton();
    }

    private void setupStepIndicator() {
        int activeColor = ContextCompat.getColor(this, R.color.gw_step_active);
        binding.stepIndicator.step1Dot.setBackgroundColor(activeColor);
        binding.stepIndicator.step2Dot.setBackgroundColor(activeColor);
    }

    private void setupGoalCards() {
        // Default: "Get stronger" pre-selected, matching the mockup
        selectGoalCard(binding.cardGoalStronger, getString(R.string.goal_get_stronger));

        binding.cardGoalStronger.setOnClickListener(v ->
                selectGoalCard(binding.cardGoalStronger, getString(R.string.goal_get_stronger)));

        binding.cardGoalMuscle.setOnClickListener(v ->
                selectGoalCard(binding.cardGoalMuscle, getString(R.string.goal_build_muscle)));

        binding.cardGoalLean.setOnClickListener(v ->
                selectGoalCard(binding.cardGoalLean, getString(R.string.goal_get_lean)));

        binding.cardGoalReduceWeight.setOnClickListener(v ->
                selectGoalCard(binding.cardGoalReduceWeight, getString(R.string.goal_reduce_bodyweight)));
    }

    /**
     * Handles single-select behavior across the 4 goal cards manually,
     * since MaterialCardView isn't checkable out of the box the way a
     * RadioButton is — deselects the previous card, highlights the new one.
     */
    private void selectGoalCard(MaterialCardView newlySelected, String goalText) {
        if (selectedGoalCard != null) {
            selectedGoalCard.setStrokeColor(ContextCompat.getColor(this, R.color.gw_border));
            selectedGoalCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gw_surface));
        }

        newlySelected.setStrokeColor(ContextCompat.getColor(this, R.color.gw_primary));
        newlySelected.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gw_primary_light));

        selectedGoalCard = newlySelected;
        selectedGoalText = goalText;
    }

    private void setupBackButton() {
        binding.btnBackYourGoal.setOnClickListener(v -> finish());
    }

    private void setupNextButton() {
        binding.btnNextYourGoal.setOnClickListener(v -> collectAndProceed());
    }

    private void collectAndProceed() {
        OnboardingSession session = OnboardingSession.getInstance();
        session.setMainGoal(selectedGoalText);
        session.setFocusAreas(getSelectedFocusAreas());

        startActivity(new Intent(this, YourLevelActivity.class));
    }

    private List<String> getSelectedFocusAreas() {
        List<String> selected = new ArrayList<>();
        int[] chipIds = {
                binding.chipChest.getId(), binding.chipBack.getId(), binding.chipLegs.getId(),
                binding.chipArms.getId(), binding.chipFullBody.getId()
        };

        for (int chipId : chipIds) {
            Chip chip = binding.chipGroupFocusAreas.findViewById(chipId);
            if (chip != null && chip.isChecked()) {
                selected.add(chip.getText().toString());
            }
        }
        return selected;
    }
}