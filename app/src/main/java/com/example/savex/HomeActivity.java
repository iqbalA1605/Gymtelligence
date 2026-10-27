package com.example.savex;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.health.connect.client.PermissionController;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.example.savex.databinding.ActivityHomeBinding;
import com.example.savex.databinding.ContentConnectTabBinding;
import com.example.savex.databinding.ContentHomeTabBinding;
import com.example.savex.databinding.ContentProfileTabBinding;
import com.example.savex.databinding.ContentReportTabBinding;
import com.example.savex.model.AiDecisionEngine;
import com.example.savex.model.DailyHealthRecord;
import com.example.savex.model.ExerciseProgress;
import com.example.savex.model.FirestoreHelper;
import com.example.savex.model.HealthDataProvider;
import com.example.savex.model.HealthTrendAnalyzer;
import com.example.savex.model.ReportDataAnalyzer;
import com.example.savex.model.TrackedExercise;
import com.example.savex.model.UserProfile;
import com.example.savex.model.WorkoutSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private ContentHomeTabBinding homeTabBinding;
    private ContentProfileTabBinding profileTabBinding;
    private ContentConnectTabBinding connectTabBinding;
    private ContentReportTabBinding reportTabBinding;

    private HealthDataProvider healthDataProvider;
    private HealthDataProvider.HealthSnapshot latestHealthSnapshot;
    private UserProfile currentProfile;
    private String displayName = "Athlete";

    private boolean healthConnectGranted = false;
    private long lastHealthConnectFetchMillis = 0;
    private static final long HEALTH_CONNECT_REFRESH_INTERVAL_MS = 60_000;
    private String lastReadinessCategory = null;

    private WorkoutSession currentSession;
    private final Map<String, Button> exerciseButtonsByName = new HashMap<>();

    private ActivityResultLauncher<Set<String>> healthConnectPermissionLauncher;
    private ActivityResultLauncher<String> stepPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        homeTabBinding = ContentHomeTabBinding.bind(findViewById(R.id.contentHomeTab));
        profileTabBinding = ContentProfileTabBinding.bind(findViewById(R.id.contentProfileTab));
        connectTabBinding = ContentConnectTabBinding.bind(findViewById(R.id.contentConnectTab));
        reportTabBinding = ContentReportTabBinding.bind(findViewById(R.id.contentReportTab));

        healthDataProvider = new HealthDataProvider(this);

        setupPermissionLaunchers();
        setupBottomNavigation();
        setupConnectTab();
        fetchUserName();
        loadUserProfileAndPopulateScreens();
    }

    private void fetchUserName() {
        new Thread(() -> {
            String name = "Athlete";
            try {
                Map<String, String> attributes = AWSMobileClient.getInstance().getUserAttributes();
                if (attributes != null && attributes.get("name") != null && !attributes.get("name").trim().isEmpty()) {
                    name = attributes.get("name").trim();
                }
            } catch (Exception e) { /* keep fallback */ }

            final String finalName = name;
            runOnUiThread(() -> {
                displayName = finalName;
                if (currentProfile != null) {
                    populateProfileTab(currentProfile);
                    homeTabBinding.textUserPlanTitle.setText(displayName + "'s Plan");
                }
            });
        }).start();
    }

    private void setupPermissionLaunchers() {
        healthConnectPermissionLauncher = registerForActivityResult(
                PermissionController.createRequestPermissionResultContract(),
                granted -> {
                    if (granted.containsAll(HealthDataProvider.requiredHealthConnectPermissions())) {
                        healthConnectGranted = true;
                        connectTabBinding.textSmartwatchStatus.setText("Connected");
                        refreshHealthData(true);
                    } else {
                        healthConnectGranted = false;
                        connectTabBinding.textSmartwatchStatus.setText("Permission denied");
                    }
                }
        );

        stepPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        connectTabBinding.textPhoneSensorStatus.setText("Connected");
                        healthDataProvider.startStepUpdates(this::onSensorStepsChanged);
                    } else {
                        connectTabBinding.textPhoneSensorStatus.setText("Permission denied");
                    }
                }
        );
    }

    private void setupConnectTab() {
        connectTabBinding.cardConnectSmartwatch.setOnClickListener(v -> {
            if (!HealthDataProvider.isHealthConnectAvailable(this)) {
                Toast.makeText(this, "Health Connect app isn't installed on this device", Toast.LENGTH_LONG).show();
                return;
            }
            healthConnectPermissionLauncher.launch(HealthDataProvider.requiredHealthConnectPermissions());
        });

        connectTabBinding.cardConnectPhone.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                stepPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
                return;
            }
            connectTabBinding.textPhoneSensorStatus.setText("Connected");
            healthDataProvider.startStepUpdates(this::onSensorStepsChanged);
        });
    }

    private void onSensorStepsChanged() {
        if (!healthConnectGranted) {
            int steps = healthDataProvider.getLatestStepCount();
            connectTabBinding.healthStatsContainer.setVisibility(View.VISIBLE);
            connectTabBinding.textStepsValue.setText("Steps today: " + steps);
            homeTabBinding.textHomeSteps.setText("Steps: " + steps);
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastHealthConnectFetchMillis >= HEALTH_CONNECT_REFRESH_INTERVAL_MS) {
            refreshHealthData(false);
        }
    }

    private void refreshHealthData(boolean showErrorToast) {
        lastHealthConnectFetchMillis = System.currentTimeMillis();
        healthDataProvider.fetchHealthConnectData(new HealthDataProvider.HealthCallback() {
            @Override
            public void onData(HealthDataProvider.HealthSnapshot snapshot) {
                runOnUiThread(() -> {
                    latestHealthSnapshot = snapshot;

                    connectTabBinding.healthStatsContainer.setVisibility(View.VISIBLE);
                    connectTabBinding.textStepsValue.setText("Steps today: " + snapshot.steps);
                    connectTabBinding.textHeartRateValue.setText(snapshot.heartRateBpm > 0 ? "Heart rate: " + snapshot.heartRateBpm + " bpm" : "Heart rate: no data yet");
                    connectTabBinding.textSleepValue.setText(snapshot.sleepHours > 0 ? "Sleep: " + String.format("%.1f", snapshot.sleepHours) + " hrs" : "Sleep: no data yet");

                    homeTabBinding.textHomeSteps.setText("Steps: " + snapshot.steps);
                    homeTabBinding.textHomeHeartRate.setText(snapshot.heartRateBpm > 0 ? "Heart rate: " + snapshot.heartRateBpm + " bpm" : "Heart rate: no data yet");
                    homeTabBinding.textHomeSleep.setText(snapshot.sleepHours > 0 ? "Sleep: " + String.format("%.1f", snapshot.sleepHours) + " hrs" : "Sleep: no data yet");

                    int readiness = AiDecisionEngine.computeReadinessScore(snapshot);
                    String category = AiDecisionEngine.readinessCategory(readiness);
                    homeTabBinding.textReadinessScore.setText(String.valueOf(readiness));
                    homeTabBinding.textReadinessMessage.setText(AiDecisionEngine.readinessMessage(readiness));

                    boolean categoryChanged = !category.equals(lastReadinessCategory);
                    lastReadinessCategory = category;
                    if (categoryChanged && currentProfile != null) {
                        buildTodaysWorkout(currentProfile);
                    }

                    recordAndAnalyzeHistory(snapshot);
                });
            }

            @Override
            public void onError(Exception e) {
                if (showErrorToast) {
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Could not read health data: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void recordAndAnalyzeHistory(HealthDataProvider.HealthSnapshot snapshot) {
        String today = HealthTrendAnalyzer.todayDateString();
        DailyHealthRecord todayRecord = new DailyHealthRecord();
        todayRecord.date = today;
        todayRecord.steps = snapshot.steps;
        todayRecord.sleepHours = snapshot.sleepHours;
        todayRecord.heartRateBpm = snapshot.heartRateBpm;

        FirestoreHelper.saveDailyHealthRecord(getApplicationContext(), todayRecord, new FirestoreHelper.WriteCallback() {
            @Override
            public void onSuccess() {
                FirestoreHelper.getRecentHealthHistory(getApplicationContext(), 7, new FirestoreHelper.HistoryCallback() {
                    @Override
                    public void onSuccess(List<DailyHealthRecord> records) {
                        runOnUiThread(() -> {
                            List<DailyHealthRecord> pastOnly = new ArrayList<>();
                            for (DailyHealthRecord r : records) if (!today.equals(r.date)) pastOnly.add(r);

                            HealthTrendAnalyzer.StepInsight stepInsight = HealthTrendAnalyzer.analyzeSteps(pastOnly);
                            if (stepInsight.achievementMessage != null) {
                                homeTabBinding.textStepInsight.setVisibility(View.VISIBLE);
                                homeTabBinding.textStepInsight.setText(stepInsight.achievementMessage);
                            }
                            if (stepInsight.todayTarget != null) {
                                DailyHealthRecord updated = new DailyHealthRecord();
                                updated.date = today;
                                updated.steps = snapshot.steps;
                                updated.sleepHours = snapshot.sleepHours;
                                updated.heartRateBpm = snapshot.heartRateBpm;
                                updated.stepTarget = stepInsight.todayTarget;
                                FirestoreHelper.saveDailyHealthRecord(getApplicationContext(), updated, new FirestoreHelper.WriteCallback() {
                                    @Override public void onSuccess() {}
                                    @Override public void onError(Exception e) {}
                                });
                            }

                            String sleepMessage = HealthTrendAnalyzer.analyzeSleep(snapshot.sleepHours, pastOnly);
                            homeTabBinding.textSleepInsight.setVisibility(View.VISIBLE);
                            homeTabBinding.textSleepInsight.setText(sleepMessage);
                        });
                    }
                    @Override public void onError(Exception e) {}
                });
            }
            @Override public void onError(Exception e) {}
        });
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            findViewById(R.id.contentHomeTab).setVisibility(id == R.id.navHome ? View.VISIBLE : View.GONE);
            findViewById(R.id.contentConnectTab).setVisibility(id == R.id.navConnect ? View.VISIBLE : View.GONE);
            findViewById(R.id.contentReportTab).setVisibility(id == R.id.navReport ? View.VISIBLE : View.GONE);
            findViewById(R.id.contentProfileTab).setVisibility(id == R.id.navProfile ? View.VISIBLE : View.GONE);
            if (id == R.id.navReport) populateReportTab();
            return true;
        });
    }

    private void loadUserProfileAndPopulateScreens() {
        FirestoreHelper.getUserProfile(getApplicationContext(), new FirestoreHelper.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> {
                    if (profile != null) {
                        currentProfile = profile;
                        if (profile.fullName != null && !profile.fullName.trim().isEmpty()) {
                            displayName = profile.fullName.trim();
                        }
                        buildTodaysWorkout(profile);
                        populateProfileTab(profile);
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Could not load your profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    // ===================== WORKOUT TRACKING =====================

    private void buildTodaysWorkout(UserProfile profile) {
        homeTabBinding.textUserPlanTitle.setText(displayName + "'s Plan");

        String today = HealthTrendAnalyzer.todayDateString();

        FirestoreHelper.getWorkoutSession(getApplicationContext(), today, new FirestoreHelper.WorkoutSessionCallback() {
            @Override
            public void onSuccess(WorkoutSession existing) {
                if (existing != null) {
                    runOnUiThread(() -> renderSession(existing));
                } else {
                    fetchCarryOverAndBuildNewSession(profile, today);
                }
            }
            @Override
            public void onError(Exception e) {
                fetchCarryOverAndBuildNewSession(profile, today);
            }
        });
    }

    private void fetchCarryOverAndBuildNewSession(UserProfile profile, String today) {
        FirestoreHelper.getRecentWorkoutSessions(getApplicationContext(), 3, new FirestoreHelper.WorkoutSessionListCallback() {
            @Override
            public void onSuccess(List<WorkoutSession> sessions) {
                List<TrackedExercise> pending = new ArrayList<>();
                for (WorkoutSession s : sessions) {
                    if (s.exercises == null) continue;
                    for (TrackedExercise ex : s.exercises) {
                        if (!ex.carryOver && "pending".equals(ex.status)) pending.add(ex);
                    }
                    if (!pending.isEmpty()) break;
                }

                FirestoreHelper.getAllExerciseProgress(getApplicationContext(), new FirestoreHelper.ExerciseProgressMapCallback() {
                    @Override
                    public void onSuccess(Map<String, ExerciseProgress> progressByName) {
                        runOnUiThread(() -> buildAndRenderNewSession(profile, today, pending, progressByName));
                    }
                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> buildAndRenderNewSession(profile, today, pending, new HashMap<>()));
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> buildAndRenderNewSession(profile, today, new ArrayList<>(), new HashMap<>()));
            }
        });
    }

    private void buildAndRenderNewSession(UserProfile profile, String today, List<TrackedExercise> pending, Map<String, ExerciseProgress> progressByName) {
        AiDecisionEngine.TrackedPlan plan = AiDecisionEngine.generateTrackedPlan(profile, latestHealthSnapshot, pending, progressByName);

        currentSession = new WorkoutSession();
        currentSession.date = today;
        currentSession.split = plan.dayBadge;
        currentSession.status = "none";
        currentSession.exercises = new ArrayList<>();
        currentSession.exercises.addAll(plan.carryOverExercises);
        currentSession.exercises.addAll(plan.exercises);

        homeTabBinding.textPlanBadge.setText(plan.dayBadge);
        homeTabBinding.textPlanTitle.setText(plan.title);
        homeTabBinding.textPlanSubtitle.setText(plan.subtitle);

        renderExercises();
        updateReadinessDisplay();
    }

    private void renderSession(WorkoutSession session) {
        currentSession = session;
        homeTabBinding.textPlanBadge.setText(session.split);
        homeTabBinding.textPlanTitle.setText(session.split.equals("Full Body") ? "Full Body Session" : session.split + " Day");
        homeTabBinding.textPlanSubtitle.setText(session.exercises.size() + " exercises");
        renderExercises();
        updateReadinessDisplay();
    }

    private void updateReadinessDisplay() {
        if (latestHealthSnapshot != null) {
            int readiness = AiDecisionEngine.computeReadinessScore(latestHealthSnapshot);
            homeTabBinding.textReadinessScore.setText(String.valueOf(readiness));
            homeTabBinding.textReadinessMessage.setText(AiDecisionEngine.readinessMessage(readiness));
        } else {
            homeTabBinding.textReadinessScore.setText("--");
            homeTabBinding.textReadinessMessage.setText("Connect a device to see your readiness score");
        }
    }

    private void renderExercises() {
        exerciseButtonsByName.clear();
        homeTabBinding.carryOverContainer.removeAllViews();
        homeTabBinding.exerciseListContainer.removeAllViews();

        boolean hasCarryOver = false;
        for (TrackedExercise ex : currentSession.exercises) {
            if (ex.carryOver) {
                hasCarryOver = true;
                homeTabBinding.carryOverContainer.addView(buildCarryOverHeaderIfNeeded());
                homeTabBinding.carryOverContainer.addView(buildExerciseRow(ex));
            }
        }
        homeTabBinding.carryOverContainer.setVisibility(hasCarryOver ? View.VISIBLE : View.GONE);

        for (TrackedExercise ex : currentSession.exercises) {
            if (!ex.carryOver) {
                homeTabBinding.exerciseListContainer.addView(buildExerciseRow(ex));
            }
        }

        homeTabBinding.btnFinishWorkout.setOnClickListener(v -> finishWorkout());
    }

    private TextView buildCarryOverHeaderIfNeeded() {
        TextView header = new TextView(this);
        header.setText("Carry-over");
        header.setTextColor(getColor(R.color.gw_text_secondary));
        header.setTextSize(11);
        header.setPadding(0, 0, 0, 8);
        return header;
    }

    private LinearLayout buildExerciseRow(TrackedExercise exercise) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 20, 0, 20);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(textParams);

        TextView nameView = new TextView(this);
        nameView.setText(exercise.name);
        nameView.setTextColor(getColor(R.color.gw_text_primary));
        nameView.setTextSize(13);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView metaView = new TextView(this);
        String weightText = exercise.weightKg > 0 ? " · " + formatWeight(exercise.weightKg) + " kg" : "";
        metaView.setText(exercise.sets + " × " + exercise.reps + weightText);
        metaView.setTextColor(getColor(R.color.gw_text_secondary));
        metaView.setTextSize(11);

        textCol.addView(nameView);
        textCol.addView(metaView);

        Button doneButton = new Button(this);
        doneButton.setAllCaps(false);
        doneButton.setTextSize(12);
        styleDoneButton(doneButton, exercise);

        doneButton.setOnClickListener(v -> {
            exercise.status = "completed".equals(exercise.status) ? "pending" : "completed";
            styleDoneButton(doneButton, exercise);
            persistSession();
        });

        exerciseButtonsByName.put(exercise.name, doneButton);

        row.addView(textCol);
        row.addView(doneButton);
        return row;
    }

    private void styleDoneButton(Button button, TrackedExercise exercise) {
        if ("completed".equals(exercise.status)) {
            button.setText("✓ Done");
            button.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_exercise_btn_completed));
            button.setTextColor(getColor(R.color.gw_text_on_primary));
        } else {
            button.setText("Done");
            button.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_exercise_btn_incomplete));
            button.setTextColor(getColor(android.R.color.white));
        }
    }

    private void persistSession() {
        if (currentSession == null) return;
        FirestoreHelper.saveWorkoutSession(getApplicationContext(), currentSession, new FirestoreHelper.WriteCallback() {
            @Override public void onSuccess() {}
            @Override public void onError(Exception e) {}
        });
    }

    private void finishWorkout() {
        if (currentSession == null) return;

        int completedCount = 0;
        for (TrackedExercise ex : currentSession.exercises) {
            if ("completed".equals(ex.status)) completedCount++;
        }
        int total = currentSession.exercises.size();
        currentSession.status = completedCount == 0 ? "none" : (completedCount == total ? "full" : "partial");

        FirestoreHelper.saveWorkoutSession(getApplicationContext(), currentSession, new FirestoreHelper.WriteCallback() {
            @Override
            public void onSuccess() {
                updateProgressionForCompletedExercises();
                runOnUiThread(() -> Toast.makeText(HomeActivity.this,
                        "Workout saved (" + currentSession.status + ")", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Could not save workout: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void updateProgressionForCompletedExercises() {
        for (TrackedExercise ex : currentSession.exercises) {
            if (!"completed".equals(ex.status) || ex.weightKg <= 0) continue;

            FirestoreHelper.getAllExerciseProgress(getApplicationContext(), new FirestoreHelper.ExerciseProgressMapCallback() {
                @Override
                public void onSuccess(Map<String, ExerciseProgress> progressByName) {
                    ExerciseProgress progress = progressByName.get(ex.name);
                    if (progress == null) {
                        progress = new ExerciseProgress();
                        progress.exerciseName = ex.name;
                        progress.lastWeight = ex.weightKg;
                        progress.consecutiveCompletions = 1;
                    } else if (Math.abs(progress.lastWeight - ex.weightKg) < 0.01) {
                        progress.consecutiveCompletions += 1;
                    } else {
                        progress.lastWeight = ex.weightKg;
                        progress.consecutiveCompletions = 1;
                    }
                    progress.lastCompletedDate = currentSession.date;
                    FirestoreHelper.saveExerciseProgress(getApplicationContext(), progress, new FirestoreHelper.WriteCallback() {
                        @Override public void onSuccess() {}
                        @Override public void onError(Exception e) {}
                    });
                }
                @Override public void onError(Exception e) {}
            });
        }
    }

    private String formatWeight(double value) {
        if (value == Math.floor(value)) return String.valueOf((int) value);
        return String.valueOf(value);
    }

    // ===================== PROFILE TAB =====================

    private void populateProfileTab(UserProfile profile) {
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

    private String estimateFitnessLevel(UserProfile profile) {
        if (profile.estimated1RM <= 0 || profile.weightKg <= 0) return "Not enough data yet";
        double ratio = profile.estimated1RM / profile.weightKg;
        if (ratio < 0.5) return "Beginner";
        if (ratio < 1.0) return "Intermediate";
        return "Advanced";
    }

    // ===================== REPORT TAB =====================

    private void populateReportTab() {
        if (currentProfile == null) return;

        FirestoreHelper.getRecentWorkoutSessions(getApplicationContext(), 30, new FirestoreHelper.WorkoutSessionListCallback() {
            @Override
            public void onSuccess(List<WorkoutSession> sessions) {
                FirestoreHelper.getRecentHealthHistory(getApplicationContext(), 30, new FirestoreHelper.HistoryCallback() {
                    @Override
                    public void onSuccess(List<DailyHealthRecord> healthHistory) {
                        FirestoreHelper.getAllExerciseProgress(getApplicationContext(), new FirestoreHelper.ExerciseProgressMapCallback() {
                            @Override
                            public void onSuccess(Map<String, ExerciseProgress> progressByName) {
                                runOnUiThread(() -> renderReport(
                                        ReportDataAnalyzer.build(currentProfile, sessions, healthHistory, progressByName, latestHealthSnapshot)));
                            }
                            @Override
                            public void onError(Exception e) {
                                runOnUiThread(() -> renderReport(
                                        ReportDataAnalyzer.build(currentProfile, sessions, healthHistory, new HashMap<>(), latestHealthSnapshot)));
                            }
                        });
                    }
                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> renderReport(
                                ReportDataAnalyzer.build(currentProfile, sessions, new ArrayList<>(), new HashMap<>(), latestHealthSnapshot)));
                    }
                });
            }
            @Override
            public void onError(Exception e) { /* leave defaults on screen */ }
        });
    }

    private void renderReport(ReportDataAnalyzer.ReportData d) {
        reportTabBinding.textOverallPercent.setText(d.overallPercent + "%");
        reportTabBinding.textOverallStatus.setText(d.overallStatus);

        reportTabBinding.textGoalText.setText(d.goalText);
        reportTabBinding.textGoalPercent.setText(d.goalProgressPercent + "% toward goal");

        if (d.strengthPercent != null) {
            reportTabBinding.textStrengthSummary.setText(d.strengthExerciseName + ": "
                    + formatW(d.strengthStart) + " → " + formatW(d.strengthCurrent) + " kg ("
                    + (d.strengthPercent >= 0 ? "+" : "") + d.strengthPercent + "%)");
            reportTabBinding.chartStrength.setValues(d.strengthChartValues);
        } else {
            reportTabBinding.textStrengthSummary.setText("Not enough data yet");
        }

        reportTabBinding.textConsistencySummary.setText(d.consistencyCompleted + "/" + d.consistencyTarget
                + " · " + d.consistencyPercent + "% · streak " + d.consistencyStreak);
        reportTabBinding.chartConsistency.setValues(d.consistencyChartValues);

        reportTabBinding.textWeightCurrent.setText(((int) d.currentWeightKg) + " kg");

        if (d.readinessScore != null) {
            reportTabBinding.textRecoverySummary.setText("Readiness: " + d.readinessScore
                    + " · Sleep: " + String.format("%.1f", d.readinessSleepHours) + " hrs"
                    + (d.readinessHeartRateBpm > 0 ? " · HR: " + d.readinessHeartRateBpm + " bpm" : ""));
            reportTabBinding.chartReadiness.setValues(d.readinessChartValues);
        } else {
            reportTabBinding.textRecoverySummary.setText("Not enough data yet");
        }

        reportTabBinding.textActivitySummary.setText("Steps today: " + d.stepsToday
                + " · Active days: " + d.activeDaysThisWeek
                + (d.avgWorkoutMinutes != null ? " · ~" + d.avgWorkoutMinutes + " min/session" : ""));
        reportTabBinding.chartActivity.setValues(d.stepsChartValues);

        reportTabBinding.prContainer.removeAllViews();
        if (d.personalRecords.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("Not enough data yet");
            none.setTextColor(getColor(R.color.report_text_muted));
            none.setTextSize(13);
            reportTabBinding.prContainer.addView(none);
        } else {
            for (String pr : d.personalRecords) {
                TextView row = new TextView(this);
                row.setText(pr);
                row.setTextColor(getColor(R.color.report_text));
                row.setTextSize(13);
                row.setPadding(0, 4, 0, 4);
                reportTabBinding.prContainer.addView(row);
            }
        }
        reportTabBinding.textLongestStreak.setText("Longest streak: " + d.longestStreakDays + " days");

        reportTabBinding.textAiSummary.setText(d.aiSummary);
        reportTabBinding.textAiRecommendation.setText(d.aiRecommendation);
    }

    private String formatW(Double v) {
        if (v == null) return "--";
        return v == Math.floor(v) ? String.valueOf(v.intValue()) : String.valueOf(v);
    }
}