package com.example.savex;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.savex.databinding.ActivityHomeBinding;
import com.example.savex.databinding.ContentHomeTabBinding;
import com.example.savex.databinding.ContentProfileTabBinding;
import com.example.savex.model.DailyPlan;
import com.example.savex.model.DynamoDbHelper;
import com.example.savex.model.PlanExercise;
import com.example.savex.model.PlanGenerator;
import com.example.savex.model.UserProfile;

import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private ContentHomeTabBinding homeTabBinding;
    private ContentProfileTabBinding profileTabBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Bind the included tab layouts individually so we can populate them directly
        homeTabBinding = ContentHomeTabBinding.bind(findViewById(R.id.contentHomeTab));
        profileTabBinding = ContentProfileTabBinding.bind(findViewById(R.id.contentProfileTab));

        setupBottomNavigation();
        loadUserProfileAndPopulateScreens();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            findViewById(R.id.contentHomeTab).setVisibility(id == R.id.navHome ? View.VISIBLE : View.GONE);
            findViewById(R.id.contentConnectTab).setVisibility(id == R.id.navConnect ? View.VISIBLE : View.GONE);
            findViewById(R.id.contentReportTab).setVisibility(id == R.id.navReport ? View.VISIBLE : View.GONE);
            findViewById(R.id.contentProfileTab).setVisibility(id == R.id.navProfile ? View.VISIBLE : View.GONE);

            return true;
        });
    }

    private void loadUserProfileAndPopulateScreens() {
        DynamoDbHelper.getUserProfile(getApplicationContext(), new DynamoDbHelper.ProfileCallback() {
            @Override
            public void onSuccess(Map<String, AttributeValue> item) {
                runOnUiThread(() -> {
                    if (item != null) {
                        UserProfile profile = mapToUserProfile(item); // convert once, reuse for both tabs
                        populateHomeTab(profile);
                        populateProfileTab(profile);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this,
                                "Could not load your profile: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    /** Converts a raw DynamoDB item back into the familiar UserProfile object. */
    private UserProfile mapToUserProfile(Map<String, AttributeValue> item) {
        UserProfile profile = new UserProfile();
        profile.gender = item.containsKey("gender") ? item.get("gender").getS() : "";
        profile.heightCm = item.containsKey("heightCm") ? Double.parseDouble(item.get("heightCm").getN()) : 0;
        profile.weightKg = item.containsKey("weightKg") ? Double.parseDouble(item.get("weightKg").getN()) : 0;
        profile.mainGoal = item.containsKey("mainGoal") ? item.get("mainGoal").getS() : "";
        profile.focusAreasCsv = item.containsKey("focusAreas") ? item.get("focusAreas").getS() : "";
        profile.benchmarkExercise = item.containsKey("benchmarkExercise") ? item.get("benchmarkExercise").getS() : "";
        profile.weightLifted = item.containsKey("weightLifted") ? Double.parseDouble(item.get("weightLifted").getN()) : 0;
        profile.repsTillTired = item.containsKey("repsTillTired") ? Integer.parseInt(item.get("repsTillTired").getN()) : 0;
        profile.estimated1RM = item.containsKey("estimated1RM") ? Double.parseDouble(item.get("estimated1RM").getN()) : 0;
        profile.daysPerWeek = item.containsKey("daysPerWeek") ? Integer.parseInt(item.get("daysPerWeek").getN()) : 0;
        profile.equipmentType = item.containsKey("equipmentType") ? item.get("equipmentType").getS() : "ALL";
        return profile;
    }

    // ===================== HOME TAB =====================

    private void populateHomeTab(UserProfile profile) {
        // No real name field exists yet — placeholder until Register screens
        // (built last, per plan) collect and store a real name.
        String displayName = "Athlete";
        homeTabBinding.textUserPlanTitle.setText(displayName + "'s Plan");

        DailyPlan plan = PlanGenerator.generateTodayPlan(profile);

        homeTabBinding.textPlanBadge.setText(plan.dayBadge);
        homeTabBinding.textPlanTitle.setText(plan.title);
        homeTabBinding.textPlanSubtitle.setText(plan.subtitle);

        homeTabBinding.exerciseListContainer.removeAllViews();
        for (PlanExercise exercise : plan.exercises) {
            addExerciseRow(exercise);
        }

        // Honest placeholder — no real sensor data exists yet (Connect tab not built).
        homeTabBinding.textReadinessScore.setText("--");
        homeTabBinding.textReadinessMessage.setText("Connect a device to see your readiness score");
    }

    private void addExerciseRow(PlanExercise exercise) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 24, 0, 24);

        TextView nameView = new TextView(this);
        nameView.setText(exercise.name);
        nameView.setTextColor(getColor(R.color.gw_text_primary));
        nameView.setTextSize(13);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameView.setLayoutParams(nameParams);

        TextView metaView = new TextView(this);
        String weightText = exercise.suggestedWeightKg > 0
                ? " · " + formatWeight(exercise.suggestedWeightKg) + " kg"
                : "";
        metaView.setText(exercise.sets + " × " + exercise.reps + weightText);
        metaView.setTextColor(getColor(R.color.gw_text_secondary));
        metaView.setTextSize(11);

        row.addView(nameView);
        row.addView(metaView);
        homeTabBinding.exerciseListContainer.addView(row);
    }

    private String formatWeight(double value) {
        if (value == Math.floor(value)) return String.valueOf((int) value);
        return String.valueOf(value);
    }

    // ===================== PROFILE TAB =====================

    private void populateProfileTab(UserProfile profile) {
        String displayName = "Athlete"; // same placeholder reasoning as Home tab
        profileTabBinding.textProfileAvatarInitial.setText(displayName.substring(0, 1));
        profileTabBinding.textProfileName.setText(displayName);

        profileTabBinding.textProfileGender.setText(profile.gender != null ? profile.gender : "-");
        profileTabBinding.textProfileHeight.setText(formatHeight(profile.heightCm));
        profileTabBinding.textProfileWeight.setText(((int) profile.weightKg) + " kg");
        profileTabBinding.textProfileFitnessLevel.setText(estimateFitnessLevel(profile));
        profileTabBinding.textProfileGoal.setText(profile.mainGoal != null ? profile.mainGoal : "-");
    }

    private String formatHeight(double heightCm) {
        int totalInches = (int) Math.round(heightCm / 2.54);
        int feet = totalInches / 12;
        int inches = totalInches % 12;
        return feet + "'" + inches + "\"";
    }

    /**
     * Auto-derived fitness level from the 1RM-to-bodyweight ratio, since
     * onboarding never asked for this directly. A commonly used rough
     * strength-standard heuristic for bench press specifically — treated
     * as an approximation, not a precise classification.
     */
    private String estimateFitnessLevel(UserProfile profile) {
        if (profile.estimated1RM <= 0 || profile.weightKg <= 0) {
            return "Not enough data yet";
        }
        double ratio = profile.estimated1RM / profile.weightKg;
        if (ratio < 0.5) return "Beginner";
        if (ratio < 1.0) return "Intermediate";
        return "Advanced";
    }
}