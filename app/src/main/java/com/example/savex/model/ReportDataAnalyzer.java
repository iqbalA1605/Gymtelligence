package com.example.savex.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure computation from already-fetched real data — no network calls, no fake values. */
public class ReportDataAnalyzer {

    public static class ReportData {
        public int overallPercent;
        public String overallStatus;

        public String goalText;
        public int goalProgressPercent;

        public String strengthExerciseName;
        public Double strengthStart, strengthCurrent;
        public Integer strengthPercent;
        public List<Float> strengthChartValues = new ArrayList<>();

        public int consistencyCompleted, consistencyTarget, consistencyPercent, consistencyStreak;
        public List<Float> consistencyChartValues = new ArrayList<>();

        public double currentWeightKg;

        public Integer readinessScore;
        public double readinessSleepHours;
        public int readinessHeartRateBpm;
        public List<Float> readinessChartValues = new ArrayList<>();

        public int stepsToday, activeDaysThisWeek;
        public Integer avgWorkoutMinutes;
        public List<Float> stepsChartValues = new ArrayList<>();

        public List<String> personalRecords = new ArrayList<>();
        public int longestStreakDays;

        public String aiSummary;
        public String aiRecommendation;
    }

    public static ReportData build(UserProfile profile,
                                   List<WorkoutSession> sessions,        // most recent first
                                   List<DailyHealthRecord> healthHistory, // most recent first
                                   Map<String, ExerciseProgress> progressByName,
                                   HealthDataProvider.HealthSnapshot todaySnapshot) {
        ReportData d = new ReportData();

        // ===== Consistency =====
        d.consistencyTarget = Math.max(1, profile.daysPerWeek);
        int completed = 0;
        List<WorkoutSession> lastWeek = sessions.size() > 7 ? sessions.subList(0, 7) : sessions;
        for (WorkoutSession s : lastWeek) {
            if ("full".equals(s.status) || "partial".equals(s.status)) completed++;
        }
        d.consistencyCompleted = completed;
        d.consistencyPercent = (int) Math.round(100.0 * completed / d.consistencyTarget);
        d.consistencyStreak = computeStreak(sessions);

        List<WorkoutSession> chronological = new ArrayList<>(lastWeek);
        Collections.reverse(chronological);
        for (WorkoutSession s : chronological) {
            d.consistencyChartValues.add(("full".equals(s.status) || "partial".equals(s.status)) ? 1f : 0f);
        }

        // ===== Strength — track the exercise with the most completions =====
        if (progressByName != null && !progressByName.isEmpty()) {
            ExerciseProgress best = null;
            for (ExerciseProgress p : progressByName.values()) {
                if (best == null || p.consecutiveCompletions > best.consecutiveCompletions) best = p;
            }
            if (best != null && best.consecutiveCompletions >= 2) {
                d.strengthExerciseName = best.exerciseName;
                List<Float> weightsOverTime = new ArrayList<>();
                List<WorkoutSession> chronoAll = new ArrayList<>(sessions);
                Collections.reverse(chronoAll);
                for (WorkoutSession s : chronoAll) {
                    if (s.exercises == null) continue;
                    for (TrackedExercise ex : s.exercises) {
                        if (ex.name.equals(best.exerciseName) && "completed".equals(ex.status) && ex.weightKg > 0) {
                            weightsOverTime.add((float) ex.weightKg);
                        }
                    }
                }
                if (weightsOverTime.size() >= 2) {
                    d.strengthChartValues = weightsOverTime;
                    d.strengthStart = (double) weightsOverTime.get(0);
                    d.strengthCurrent = (double) weightsOverTime.get(weightsOverTime.size() - 1);
                    d.strengthPercent = (int) Math.round(100.0 * (d.strengthCurrent - d.strengthStart) / d.strengthStart);
                }
            }
        }

        // ===== Body weight — current only, no fabricated history =====
        d.currentWeightKg = profile.weightKg;

        // ===== Recovery / Readiness =====
        if (todaySnapshot != null) {
            d.readinessScore = AiDecisionEngine.computeReadinessScore(todaySnapshot);
            d.readinessSleepHours = todaySnapshot.sleepHours;
            d.readinessHeartRateBpm = todaySnapshot.heartRateBpm;
        }
        List<DailyHealthRecord> healthChrono = new ArrayList<>(healthHistory);
        Collections.reverse(healthChrono);
        for (DailyHealthRecord r : healthChrono) {
            if (r.steps > 0 || r.sleepHours > 0) {
                HealthDataProvider.HealthSnapshot snap = new HealthDataProvider.HealthSnapshot();
                snap.steps = r.steps;
                snap.sleepHours = r.sleepHours;
                snap.heartRateBpm = r.heartRateBpm;
                d.readinessChartValues.add((float) AiDecisionEngine.computeReadinessScore(snap));
            }
        }

        // ===== Activity =====
        d.stepsToday = todaySnapshot != null ? todaySnapshot.steps : 0;
        int activeDays = 0;
        for (DailyHealthRecord r : healthHistory) if (r.steps > 0) activeDays++;
        d.activeDaysThisWeek = Math.min(activeDays, 7);
        for (DailyHealthRecord r : healthChrono) d.stepsChartValues.add((float) r.steps);

        int totalMinutes = 0, sessionsWithData = 0;
        for (WorkoutSession s : lastWeek) {
            if (s.exercises == null || s.exercises.isEmpty()) continue;
            int sets = 0;
            for (TrackedExercise ex : s.exercises) sets += ex.sets;
            totalMinutes += Math.round(sets * 2.5f + 10);
            sessionsWithData++;
        }
        d.avgWorkoutMinutes = sessionsWithData > 0 ? totalMinutes / sessionsWithData : null;

        // ===== Personal Records =====
        java.util.Map<String, Double> bestWeights = new java.util.HashMap<>();
        for (WorkoutSession s : sessions) {
            if (s.exercises == null) continue;
            for (TrackedExercise ex : s.exercises) {
                if ("completed".equals(ex.status) && ex.weightKg > 0) {
                    Double current = bestWeights.get(ex.name);
                    if (current == null || ex.weightKg > current) bestWeights.put(ex.name, ex.weightKg);
                }
            }
        }
        for (Map.Entry<String, Double> e : bestWeights.entrySet()) {
            d.personalRecords.add(e.getKey() + ": " + formatWeight(e.getValue()) + " kg");
        }
        d.longestStreakDays = computeLongestStreak(sessions);

        // ===== Goal & Overall =====
        d.goalText = profile.mainGoal != null ? profile.mainGoal : "Not set";
        int strengthComponent = d.strengthPercent != null ? Math.max(0, Math.min(100, d.strengthPercent + 50)) : 50;
        d.goalProgressPercent = (int) Math.round((d.consistencyPercent * 0.6) + (strengthComponent * 0.4));
        d.goalProgressPercent = Math.max(0, Math.min(100, d.goalProgressPercent));
        d.overallPercent = d.goalProgressPercent;
        d.overallStatus = d.overallPercent >= 60 ? "On Track" : "Needs Attention";

        // ===== AI Summary =====
        d.aiSummary = buildAiSummary(d);
        d.aiRecommendation = buildAiRecommendation(d);

        return d;
    }

    private static int computeStreak(List<WorkoutSession> sessionsMostRecentFirst) {
        int streak = 0;
        for (WorkoutSession s : sessionsMostRecentFirst) {
            if ("full".equals(s.status) || "partial".equals(s.status)) streak++;
            else break;
        }
        return streak;
    }

    private static int computeLongestStreak(List<WorkoutSession> sessionsMostRecentFirst) {
        int longest = 0, current = 0;
        List<WorkoutSession> chrono = new ArrayList<>(sessionsMostRecentFirst);
        Collections.reverse(chrono);
        for (WorkoutSession s : chrono) {
            if ("full".equals(s.status) || "partial".equals(s.status)) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return longest;
    }

    private static String formatWeight(double value) {
        if (value == Math.floor(value)) return String.valueOf((int) value);
        return String.valueOf(value);
    }

    private static String buildAiSummary(ReportData d) {
        StringBuilder sb = new StringBuilder();
        if (d.strengthPercent != null && d.strengthPercent > 0) {
            sb.append("Your ").append(d.strengthExerciseName).append(" has improved ").append(d.strengthPercent).append("% recently. ");
        }
        if (d.consistencyPercent >= 70) {
            sb.append("Your workout consistency is strong at ").append(d.consistencyPercent).append("%. ");
        } else if (d.consistencyPercent > 0) {
            sb.append("Your workout consistency is at ").append(d.consistencyPercent).append("% — there's room to build a steadier habit. ");
        }
        sb.append("You're currently ").append(d.overallStatus.toLowerCase()).append(" toward your goal.");
        return sb.toString();
    }

    private static String buildAiRecommendation(ReportData d) {
        if (d.consistencyPercent < 60) return "Focus on hitting your weekly session count — consistency is the biggest lever right now.";
        if (d.strengthPercent == null) return "Keep completing exercises fully so we can start tracking your strength trend.";
        if (d.readinessScore != null && d.readinessScore < 60) return "Recovery looks low lately — prioritize sleep before pushing harder.";
        return "You're on a good track — consider a slightly heavier session on your best-recovered days.";
    }
}