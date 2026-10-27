package com.example.savex.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based personalized decision engine. Every plan is computed live,
 * from a single user's own onboarding data (and, when connected, their own
 * live health data) only — no training data, no other users' information
 * is ever used or needed.
 *
 * Design note: the readiness SCORE can update frequently (cheap to compute),
 * but the workout PLAN only changes when the readiness CATEGORY changes
 * (Low / Moderate / High). Exercises within each split also rotate weekly
 * so the same session doesn't repeat indefinitely.
 */
public class AiDecisionEngine {

    private static final double RATIO_SQUAT = 1.25;
    private static final double RATIO_DEADLIFT = 1.5;
    private static final double RATIO_BENCH = 1.0;
    private static final double RATIO_OVERHEAD_PRESS = 0.6;
    private static final double RATIO_ROW = 0.9;

    public static DailyPlan generateTodayPlan(UserProfile profile) {
        return generateTodayPlan(profile, null);
    }

    public static DailyPlan generateTodayPlan(UserProfile profile, HealthDataProvider.HealthSnapshot health) {
        String split = resolveSplitForToday(profile.daysPerWeek);
        List<String> focusAreas = parseFocusAreas(profile.focusAreasCsv);
        GoalProfile goalProfile = resolveGoalProfile(profile.mainGoal);

        if (health != null) {
            String category = readinessCategory(computeReadinessScore(health));
            applyReadinessAdjustment(goalProfile, category);
        }

        List<PlanExercise> exercises = buildExercises(profile, split, focusAreas, goalProfile);
        String title = split.equals("Full Body") ? "Full Body Session" : split + " Day";
        String subtitle = buildSubtitle(exercises.size(), goalProfile);

        return new DailyPlan(split, title, subtitle, exercises);
    }

    private static void applyReadinessAdjustment(GoalProfile goalProfile, String category) {
        switch (category) {
            case "Low":
                goalProfile.sets = Math.max(2, goalProfile.sets - 1);
                goalProfile.workingPercentOf1RM *= 0.85;
                goalProfile.intensityLabel = "Reduced intensity (low readiness)";
                break;
            case "High":
                break;
            case "Moderate":
            default:
                break;
        }
    }

    public static int computeReadinessScore(HealthDataProvider.HealthSnapshot health) {
        int score = 70;

        if (health.sleepHours >= 7) score += 15;
        else if (health.sleepHours >= 6) score += 5;
        else if (health.sleepHours > 0) score -= 15;

        if (health.heartRateBpm > 0 && health.heartRateBpm <= 70) score += 10;
        else if (health.heartRateBpm > 90) score -= 10;

        if (health.steps >= 10000) score -= 5;
        else if (health.steps >= 5000) score += 5;

        return Math.max(0, Math.min(100, score));
    }

    public static String readinessCategory(int score) {
        if (score >= 80) return "High";
        if (score >= 60) return "Moderate";
        return "Low";
    }

    public static String readinessMessage(int score) {
        String category = readinessCategory(score);
        switch (category) {
            case "High": return "You're well recovered — great day for a hard session";
            case "Moderate": return "Decent recovery — train as planned";
            default: return "Recovery looks low — today's session has been lightened for you";
        }
    }

    // ===================== SUBTITLE (exercise count, duration, intensity) =====================

    private static String buildSubtitle(int exerciseCount, GoalProfile goalProfile) {
        int estimatedMinutes = (int) Math.round((exerciseCount * goalProfile.sets * 2.5 + 10) / 5.0) * 5;
        return exerciseCount + " exercises · ~" + estimatedMinutes + " min · " + goalProfile.intensityLabel;
    }

    // ===================== SPLIT LOGIC (daysPerWeek) =====================

    private static String resolveSplitForToday(int daysPerWeek) {
        String[] split;
        if (daysPerWeek <= 3) {
            split = new String[]{"Full Body", "Full Body", "Full Body"};
        } else if (daysPerWeek == 4) {
            split = new String[]{"Upper", "Lower", "Upper", "Lower"};
        } else if (daysPerWeek == 5) {
            split = new String[]{"Push", "Pull", "Legs", "Upper", "Lower"};
        } else {
            split = new String[]{"Push", "Pull", "Legs", "Push", "Pull", "Legs"};
        }
        int dayIndex = (int) ((System.currentTimeMillis() / (1000 * 60 * 60 * 24)) % split.length);
        return split[dayIndex];
    }

    // ===================== GOAL LOGIC (mainGoal) =====================

    private static class GoalProfile {
        String label, intensityLabel;
        int sets, repsLow, repsHigh;
        double workingPercentOf1RM;
    }

    private static GoalProfile resolveGoalProfile(String mainGoal) {
        GoalProfile g = new GoalProfile();
        if (mainGoal == null) mainGoal = "";

        switch (mainGoal.toLowerCase()) {
            case "get stronger":
                g.label = "Strength Focus"; g.sets = 5; g.repsLow = 3; g.repsHigh = 6;
                g.workingPercentOf1RM = 0.85; g.intensityLabel = "High intensity";
                break;
            case "build muscle":
                g.label = "Muscle Building"; g.sets = 4; g.repsLow = 8; g.repsHigh = 12;
                g.workingPercentOf1RM = 0.70; g.intensityLabel = "Moderate-high intensity";
                break;
            case "lose weight":
            case "reduce weight":
                g.label = "Fat Loss"; g.sets = 3; g.repsLow = 12; g.repsHigh = 15;
                g.workingPercentOf1RM = 0.55; g.intensityLabel = "Light-moderate intensity";
                break;
            case "get lean":
                g.label = "Lean & Toned"; g.sets = 3; g.repsLow = 10; g.repsHigh = 14;
                g.workingPercentOf1RM = 0.60; g.intensityLabel = "Light-moderate intensity";
                break;
            default:
                g.label = "General Fitness"; g.sets = 3; g.repsLow = 8; g.repsHigh = 12;
                g.workingPercentOf1RM = 0.65; g.intensityLabel = "Moderate intensity";
        }
        return g;
    }

    // ===================== FOCUS AREAS =====================

    private static List<String> parseFocusAreas(String csv) {
        List<String> areas = new ArrayList<>();
        if (csv != null && !csv.trim().isEmpty()) {
            for (String s : csv.split(",")) areas.add(s.trim());
        }
        return areas;
    }

    // ===================== EXERCISE SELECTION (equipment + split + focus + weekly rotation) =====================

    private static List<PlanExercise> buildExercises(UserProfile profile, String split,
                                                     List<String> focusAreas, GoalProfile goalProfile) {
        List<PlanExercise> exercises = new ArrayList<>();
        String equipment = profile.equipmentType != null ? profile.equipmentType : "ALL";

        List<ExerciseTemplate> templates = getTemplatesForSplit(split, equipment);

        if (!focusAreas.isEmpty()) {
            templates.sort((a, b) -> {
                boolean aMatch = focusAreas.stream().anyMatch(f -> a.bodyPart.equalsIgnoreCase(f));
                boolean bMatch = focusAreas.stream().anyMatch(f -> b.bodyPart.equalsIgnoreCase(f));
                return Boolean.compare(bMatch, aMatch);
            });
        }

        for (ExerciseTemplate t : templates) {
            int reps = t.isCompound ? goalProfile.repsLow : goalProfile.repsHigh;
            double weight = estimateWeight(profile, t, goalProfile);
            exercises.add(new PlanExercise(t.name, goalProfile.sets, reps, weight));
        }
        return exercises;
    }

    private static class ExerciseTemplate {
        String name, bodyPart;
        double ratioToUserBenchmark;
        boolean isCompound;

        ExerciseTemplate(String name, String bodyPart, double ratioToUserBenchmark, boolean isCompound) {
            this.name = name; this.bodyPart = bodyPart;
            this.ratioToUserBenchmark = ratioToUserBenchmark; this.isCompound = isCompound;
        }
    }

    private static List<ExerciseTemplate> getTemplatesForSplit(String split, String equipment) {
        boolean bodyweightOnly = "BODYWEIGHT".equalsIgnoreCase(equipment);
        boolean dumbbellsOnly = "DUMBBELLS".equalsIgnoreCase(equipment);
        boolean machinesOnly = "MACHINES".equalsIgnoreCase(equipment);

        // Rotates every 7 days so the same split doesn't show identical exercises indefinitely.
        int weekIndex = (int) (System.currentTimeMillis() / (7L * 24 * 60 * 60 * 1000));

        List<ExerciseTemplate> list = new ArrayList<>();

        switch (split) {
            case "Push": {
                List<ExerciseTemplate> chest = bodyweightOnly
                        ? List.of(new ExerciseTemplate("Push-ups", "Chest", RATIO_BENCH, true))
                        : List.of(
                        new ExerciseTemplate("Bench Press", "Chest", RATIO_BENCH, true),
                        new ExerciseTemplate("Incline Dumbbell Press", "Chest", RATIO_BENCH * 0.85, true),
                        new ExerciseTemplate(machinesOnly ? "Chest Press Machine" : "Dumbbell Fly", "Chest", RATIO_BENCH * 0.6, false));
                List<ExerciseTemplate> shoulders = List.of(
                        new ExerciseTemplate(machinesOnly ? "Shoulder Press Machine" : "Overhead Press", "Shoulders", RATIO_OVERHEAD_PRESS, true),
                        new ExerciseTemplate("Lateral Raise", "Shoulders", RATIO_OVERHEAD_PRESS * 0.4, false));
                List<ExerciseTemplate> arms = List.of(
                        new ExerciseTemplate(bodyweightOnly ? "Dips" : "Tricep Pushdown", "Arms", RATIO_OVERHEAD_PRESS * 0.6, false),
                        new ExerciseTemplate("Overhead Tricep Extension", "Arms", RATIO_OVERHEAD_PRESS * 0.5, false));

                list.add(chest.get(weekIndex % chest.size()));
                list.add(shoulders.get(weekIndex % shoulders.size()));
                list.add(arms.get(weekIndex % arms.size()));
                break;
            }
            case "Pull": {
                List<ExerciseTemplate> back1 = bodyweightOnly
                        ? List.of(new ExerciseTemplate("Pull-ups", "Back", RATIO_ROW, true))
                        : List.of(
                        new ExerciseTemplate("Barbell Row", "Back", RATIO_ROW, true),
                        new ExerciseTemplate("T-Bar Row", "Back", RATIO_ROW * 0.9, true));
                List<ExerciseTemplate> back2 = List.of(
                        new ExerciseTemplate(dumbbellsOnly ? "Dumbbell Row" : "Lat Pulldown", "Back", RATIO_ROW * 0.85, true),
                        new ExerciseTemplate("Seated Cable Row", "Back", RATIO_ROW * 0.8, true));
                List<ExerciseTemplate> arms = List.of(
                        new ExerciseTemplate("Bicep Curl", "Arms", RATIO_OVERHEAD_PRESS * 0.5, false),
                        new ExerciseTemplate("Hammer Curl", "Arms", RATIO_OVERHEAD_PRESS * 0.5, false));

                list.add(back1.get(weekIndex % back1.size()));
                list.add(back2.get(weekIndex % back2.size()));
                list.add(arms.get(weekIndex % arms.size()));
                break;
            }
            case "Legs": {
                List<ExerciseTemplate> quads = bodyweightOnly
                        ? List.of(new ExerciseTemplate("Bodyweight Squats", "Legs", RATIO_SQUAT, true))
                        : List.of(
                        new ExerciseTemplate("Squat", "Legs", RATIO_SQUAT, true),
                        new ExerciseTemplate("Leg Press", "Legs", RATIO_SQUAT * 1.1, true));
                List<ExerciseTemplate> posterior = bodyweightOnly
                        ? List.of(new ExerciseTemplate("Lunges", "Legs", RATIO_DEADLIFT * 0.6, true))
                        : List.of(
                        new ExerciseTemplate("Romanian Deadlift", "Legs", RATIO_DEADLIFT * 0.8, true),
                        new ExerciseTemplate("Leg Curl", "Legs", RATIO_DEADLIFT * 0.5, false));
                List<ExerciseTemplate> calves = List.of(
                        new ExerciseTemplate("Calf Raise", "Legs", RATIO_SQUAT * 0.5, false),
                        new ExerciseTemplate("Seated Calf Raise", "Legs", RATIO_SQUAT * 0.45, false));

                list.add(quads.get(weekIndex % quads.size()));
                list.add(posterior.get(weekIndex % posterior.size()));
                list.add(calves.get(weekIndex % calves.size()));
                break;
            }
            case "Upper": {
                List<ExerciseTemplate> chest = bodyweightOnly
                        ? List.of(new ExerciseTemplate("Push-ups", "Chest", RATIO_BENCH, true))
                        : List.of(
                        new ExerciseTemplate("Bench Press", "Chest", RATIO_BENCH, true),
                        new ExerciseTemplate("Incline Dumbbell Press", "Chest", RATIO_BENCH * 0.85, true));
                List<ExerciseTemplate> back = bodyweightOnly
                        ? List.of(new ExerciseTemplate("Pull-ups", "Back", RATIO_ROW, true))
                        : List.of(
                        new ExerciseTemplate("Barbell Row", "Back", RATIO_ROW, true),
                        new ExerciseTemplate("Lat Pulldown", "Back", RATIO_ROW * 0.85, true));
                List<ExerciseTemplate> shoulders = List.of(
                        new ExerciseTemplate(machinesOnly ? "Shoulder Press Machine" : "Overhead Press", "Shoulders", RATIO_OVERHEAD_PRESS, true),
                        new ExerciseTemplate("Lateral Raise", "Shoulders", RATIO_OVERHEAD_PRESS * 0.4, false));

                list.add(chest.get(weekIndex % chest.size()));
                list.add(back.get(weekIndex % back.size()));
                list.add(shoulders.get(weekIndex % shoulders.size()));
                break;
            }
            case "Lower": {
                List<ExerciseTemplate> quads = bodyweightOnly
                        ? List.of(new ExerciseTemplate("Bodyweight Squats", "Legs", RATIO_SQUAT, true))
                        : List.of(
                        new ExerciseTemplate("Squat", "Legs", RATIO_SQUAT, true),
                        new ExerciseTemplate("Leg Press", "Legs", RATIO_SQUAT * 1.1, true));
                List<ExerciseTemplate> posterior = bodyweightOnly
                        ? List.of(new ExerciseTemplate("Glute Bridge", "Legs", RATIO_DEADLIFT * 0.6, true))
                        : List.of(new ExerciseTemplate("Deadlift", "Legs", RATIO_DEADLIFT, true));
                List<ExerciseTemplate> calves = List.of(
                        new ExerciseTemplate("Calf Raise", "Legs", RATIO_SQUAT * 0.5, false),
                        new ExerciseTemplate("Seated Calf Raise", "Legs", RATIO_SQUAT * 0.45, false));

                list.add(quads.get(weekIndex % quads.size()));
                list.add(posterior.get(weekIndex % posterior.size()));
                list.add(calves.get(weekIndex % calves.size()));
                break;
            }
            default: { // Full Body
                List<ExerciseTemplate> legs = bodyweightOnly
                        ? List.of(new ExerciseTemplate("Bodyweight Squats", "Legs", RATIO_SQUAT, true))
                        : List.of(new ExerciseTemplate("Squat", "Legs", RATIO_SQUAT, true));
                List<ExerciseTemplate> chest = bodyweightOnly
                        ? List.of(new ExerciseTemplate("Push-ups", "Chest", RATIO_BENCH, true))
                        : List.of(new ExerciseTemplate("Bench Press", "Chest", RATIO_BENCH, true));
                List<ExerciseTemplate> back = bodyweightOnly
                        ? List.of(new ExerciseTemplate("Pull-ups", "Back", RATIO_ROW, true))
                        : List.of(new ExerciseTemplate("Barbell Row", "Back", RATIO_ROW, true));

                list.add(legs.get(weekIndex % legs.size()));
                list.add(chest.get(weekIndex % chest.size()));
                list.add(back.get(weekIndex % back.size()));
                list.add(new ExerciseTemplate("Plank", "Core", 0, false));
            }
        }

        return list;
    }

    // ===================== WEIGHT ESTIMATION (benchmarkExercise + estimated1RM) =====================

    private static double estimateWeight(UserProfile profile, ExerciseTemplate template, GoalProfile goalProfile) {
        if (template.ratioToUserBenchmark == 0 || profile.estimated1RM <= 0) {
            return 0;
        }

        double benchmarkRatio = getBenchmarkRatio(profile.benchmarkExercise);
        double normalizedBase1RM = profile.estimated1RM / benchmarkRatio;
        double exercise1RM = normalizedBase1RM * template.ratioToUserBenchmark;
        double workingWeight = exercise1RM * goalProfile.workingPercentOf1RM;

        return roundToNearest(workingWeight, 1.0);
    }

    private static double getBenchmarkRatio(String benchmarkExercise) {
        if (benchmarkExercise == null) return RATIO_BENCH;
        switch (benchmarkExercise.toLowerCase()) {
            case "squat": return RATIO_SQUAT;
            case "deadlift": return RATIO_DEADLIFT;
            case "overhead press": return RATIO_OVERHEAD_PRESS;
            case "bench press":
            default: return RATIO_BENCH;
        }
    }

    private static double roundToNearest(double value, double increment) {
        return Math.round(value / increment) * increment;
    }

    public static class TrackedPlan {
        public String dayBadge, title, subtitle;
        public List<TrackedExercise> carryOverExercises = new ArrayList<>();
        public List<TrackedExercise> exercises = new ArrayList<>();
    }

    public static TrackedPlan generateTrackedPlan(UserProfile profile, HealthDataProvider.HealthSnapshot health,
                                                  List<TrackedExercise> pendingCarryOver,
                                                  java.util.Map<String, ExerciseProgress> progressByName) {
        String split = resolveSplitForToday(profile.daysPerWeek);
        List<String> focusAreas = parseFocusAreas(profile.focusAreasCsv);
        GoalProfile goalProfile = resolveGoalProfile(profile.mainGoal);

        if (health != null) {
            applyReadinessAdjustment(goalProfile, readinessCategory(computeReadinessScore(health)));
        }

        List<ExerciseTemplate> templates = new ArrayList<>(getTemplatesForSplit(split, profile.equipmentType != null ? profile.equipmentType : "ALL"));
        if (!focusAreas.isEmpty()) {
            templates.sort((a, b) -> {
                boolean aMatch = focusAreas.stream().anyMatch(f -> a.bodyPart.equalsIgnoreCase(f));
                boolean bMatch = focusAreas.stream().anyMatch(f -> b.bodyPart.equalsIgnoreCase(f));
                return Boolean.compare(bMatch, aMatch);
            });
        }

        TrackedPlan plan = new TrackedPlan();
        plan.dayBadge = split;
        plan.title = split.equals("Full Body") ? "Full Body Session" : split + " Day";

        // Carry-over: pick the first pending exercise whose body part fits today's split, if any.
        TrackedExercise matchedCarryOver = null;
        if (pendingCarryOver != null) {
            for (TrackedExercise pending : pendingCarryOver) {
                if (splitAllowsBodyPart(split, pending.bodyPart)) {
                    matchedCarryOver = pending;
                    break;
                }
            }
        }
        if (matchedCarryOver != null) {
            TrackedExercise carry = new TrackedExercise(matchedCarryOver.name, matchedCarryOver.bodyPart,
                    matchedCarryOver.sets, matchedCarryOver.reps, matchedCarryOver.weightKg, true);
            plan.carryOverExercises.add(carry);

            // Replace one same-body-part planned exercise so total workload doesn't increase.
            for (int i = 0; i < templates.size(); i++) {
                if (templates.get(i).bodyPart.equalsIgnoreCase(matchedCarryOver.bodyPart)) {
                    templates.remove(i);
                    break;
                }
            }
        }

        for (ExerciseTemplate t : templates) {
            int reps = t.isCompound ? goalProfile.repsLow : goalProfile.repsHigh;
            double weight = estimateWeightWithProgression(profile, t, goalProfile, progressByName);
            plan.exercises.add(new TrackedExercise(t.name, t.bodyPart, goalProfile.sets, reps, weight, false));
        }

        int totalCount = plan.exercises.size() + plan.carryOverExercises.size();
        plan.subtitle = buildSubtitle(totalCount, goalProfile);
        return plan;
    }

    private static boolean splitAllowsBodyPart(String split, String bodyPart) {
        if (bodyPart == null) return false;
        switch (split) {
            case "Push": case "Upper":
                return bodyPart.equalsIgnoreCase("Chest") || bodyPart.equalsIgnoreCase("Shoulders") || bodyPart.equalsIgnoreCase("Arms");
            case "Pull":
                return bodyPart.equalsIgnoreCase("Back") || bodyPart.equalsIgnoreCase("Arms");
            case "Legs": case "Lower":
                return bodyPart.equalsIgnoreCase("Legs");
            default: // Full Body
                return true;
        }
    }

    private static double estimateWeightWithProgression(UserProfile profile, ExerciseTemplate template,
                                                        GoalProfile goalProfile,
                                                        java.util.Map<String, ExerciseProgress> progressByName) {
        double formulaWeight = estimateWeight(profile, template, goalProfile);
        if (formulaWeight == 0 || progressByName == null) return formulaWeight;

        ExerciseProgress progress = progressByName.get(template.name);
        if (progress != null && progress.consecutiveCompletions >= 2) {
            // Completed this exercise successfully 2+ times in a row at the same weight -> small overload bump.
            double bumped = roundToNearest(progress.lastWeight * 1.05, 1.0);
            return Math.max(formulaWeight, bumped);
        }
        return formulaWeight;
    }
}