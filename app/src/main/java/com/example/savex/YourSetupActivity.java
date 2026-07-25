package com.example.savex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.savex.databinding.ActivityYourSetupBinding;
import com.example.savex.model.DynamoDbHelper;
import com.example.savex.model.OnboardingSession;
import com.example.savex.model.UserProfile;
import com.google.android.material.card.MaterialCardView;

public class YourSetupActivity extends AppCompatActivity {

    private ActivityYourSetupBinding binding;

    private MaterialCardView selectedDaysCard;
    private int selectedDays = 4; // default matches the mockup

    private MaterialCardView selectedEquipmentCard;
    private OnboardingSession.EquipmentType selectedEquipment = OnboardingSession.EquipmentType.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityYourSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupStepIndicator();
        setupDaysCards();
        setupEquipmentCards();
        setupBackButton();
        setupGenerateButton();
    }

    private void setupStepIndicator() {
        int activeColor = ContextCompat.getColor(this, R.color.gw_step_active);
        binding.stepIndicator.step1Dot.setBackgroundColor(activeColor);
        binding.stepIndicator.step2Dot.setBackgroundColor(activeColor);
        binding.stepIndicator.step3Dot.setBackgroundColor(activeColor);
        binding.stepIndicator.step4Dot.setBackgroundColor(activeColor);
    }

    private void setupDaysCards() {
        selectDaysCard(binding.cardDays4, 4); // pre-selected default

        binding.cardDays4.setOnClickListener(v -> selectDaysCard(binding.cardDays4, 4));
        binding.cardDays5.setOnClickListener(v -> selectDaysCard(binding.cardDays5, 5));
    }

    private void selectDaysCard(MaterialCardView newlySelected, int days) {
        if (selectedDaysCard != null) {
            selectedDaysCard.setStrokeColor(ContextCompat.getColor(this, R.color.gw_border));
            selectedDaysCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gw_surface));
        }
        newlySelected.setStrokeColor(ContextCompat.getColor(this, R.color.gw_primary));
        newlySelected.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gw_primary_light));

        selectedDaysCard = newlySelected;
        selectedDays = days;
    }

    private void setupEquipmentCards() {
        selectEquipmentCard(binding.cardEquipAll, OnboardingSession.EquipmentType.ALL); // default

        binding.cardEquipAll.setOnClickListener(v ->
                selectEquipmentCard(binding.cardEquipAll, OnboardingSession.EquipmentType.ALL));
        binding.cardEquipDumbbells.setOnClickListener(v ->
                selectEquipmentCard(binding.cardEquipDumbbells, OnboardingSession.EquipmentType.DUMBBELLS));
        binding.cardEquipBodyweight.setOnClickListener(v ->
                selectEquipmentCard(binding.cardEquipBodyweight, OnboardingSession.EquipmentType.BODYWEIGHT));
        binding.cardEquipMachines.setOnClickListener(v ->
                selectEquipmentCard(binding.cardEquipMachines, OnboardingSession.EquipmentType.MACHINES));
    }

    private void selectEquipmentCard(MaterialCardView newlySelected, OnboardingSession.EquipmentType type) {
        if (selectedEquipmentCard != null) {
            selectedEquipmentCard.setStrokeColor(ContextCompat.getColor(this, R.color.gw_border));
            selectedEquipmentCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gw_surface));
        }
        newlySelected.setStrokeColor(ContextCompat.getColor(this, R.color.gw_primary));
        newlySelected.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gw_primary_light));

        selectedEquipmentCard = newlySelected;
        selectedEquipment = type;
    }

    private void setupBackButton() {
        binding.btnBackYourSetup.setOnClickListener(v -> finish());
    }

    private void setupGenerateButton() {
        binding.btnGeneratePlan.setOnClickListener(v -> finishOnboarding());
    }

    /**
     * The single most important method in the onboarding flow:
     * saves the final screen's data to DynamoDB (keyed to the signed-in
     * user's Cognito identity) and routes to HomeActivity. From this point
     * on, MainActivity's router will find this profile and always skip
     * onboarding for this user.
     */
    private void finishOnboarding() {
        OnboardingSession session = OnboardingSession.getInstance();
        session.setDaysPerWeek(selectedDays);
        session.setEquipmentType(selectedEquipment);

        UserProfile profile = new UserProfile();
        profile.gender = session.getGender();
        profile.heightCm = session.getHeightCm();
        profile.weightKg = session.getWeightKg();
        profile.mainGoal = session.getMainGoal();
        profile.focusAreasCsv = String.join(",", session.getFocusAreas());
        profile.benchmarkExercise = session.getBenchmarkExercise();
        profile.weightLifted = session.getWeightLifted();
        profile.repsTillTired = session.getRepsTillTired();
        profile.estimated1RM = session.getEstimated1RM();
        profile.daysPerWeek = session.getDaysPerWeek();
        profile.equipmentType = session.getEquipmentType() != null
                ? session.getEquipmentType().name()
                : OnboardingSession.EquipmentType.ALL.name();

        DynamoDbHelper.saveUserProfile(getApplicationContext(), profile, new DynamoDbHelper.WriteCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Intent intent = new Intent(YourSetupActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(YourSetupActivity.this,
                                "Could not save your plan: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }
}