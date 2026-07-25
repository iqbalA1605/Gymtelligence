package com.example.savex.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure calculation class — no ML, no stored history. Takes the user's
 * onboarding answers and computes today's plan fresh, every time.
 * Matches the "recent input in, plan out" design agreed on earlier —
 * NOT a trained model, just a formula.
 */
public class PlanGenerator {

    public static DailyPlan generateTodayPlan(UserProfile profile) {
        String goal = profile.mainGoal != null ? profile.mainGoal : "Get stronger";
        String equipment = profile.equipmentType != null ? profile.equipmentType : "ALL";
        double oneRepMax = profile.estimated1RM;

        // Intensity target as a % of 1RM, based on goal — standard strength-training ranges
        double intensityPercent = getIntensityPercentForGoal(goal);
        int sets = getSetsForGoal(goal);
        int reps = getRepsForGoal(goal);

        List<PlanExercise> exercises = new ArrayList<>();

        boolean hasBarbell = equipment.equals("ALL");
        boolean hasDumbbells = equipment.equals("ALL") || equipment.equals("DUMBBELLS");
        boolean hasMachines = equipment.equals("ALL") || equipment.equals("MACHINES");

        // Cold-start handling: if no 1RM was ever estimated (user hit
        // "I'm a beginner and I don't know"), oneRepMax is 0 — use a
        // conservative flat suggested weight instead of a % calculation
        // that would otherwise multiply out to zero.
        double baseWeight = oneRepMax > 0 ? oneRepMax * intensityPercent : 0;

        if (hasBarbell) {
            exercises.add(new PlanExercise("Bench Press", sets, reps, roundToNearestHalf(baseWeight)));
        } else if (hasDumbbells) {
            exercises.add(new PlanExercise("Dumbbell Chest Press", sets, reps, roundToNearestHalf(baseWeight * 0.4)));
        } else {
            exercises.add(new PlanExercise("Push-ups", sets, reps + 4, 0));
        }

        if (hasBarbell) {
            exercises.add(new PlanExercise("Overhead Press", sets, reps + 2, roundToNearestHalf(baseWeight * 0.55)));
        } else if (hasDumbbells) {
            exercises.add(new PlanExercise("Dumbbell Shoulder Press", sets, reps + 2, roundToNearestHalf(baseWeight * 0.22)));
        } else {
            exercises.add(new PlanExercise("Pike Push-ups", sets, reps + 2, 0));
        }

        if (hasDumbbells) {
            exercises.add(new PlanExercise("Incline Dumbbell Press", sets, reps + 2, roundToNearestHalf(baseWeight * 0.35)));
        } else if (hasMachines) {
            exercises.add(new PlanExercise("Chest Press Machine", sets, reps + 2, roundToNearestHalf(baseWeight * 0.6)));
        } else {
            exercises.add(new PlanExercise("Incline Push-ups", sets, reps + 2, 0));
        }

        if (hasMachines) {
            exercises.add(new PlanExercise("Tricep Pushdown", sets, reps + 4, roundToNearestHalf(baseWeight * 0.3)));
        } else if (hasDumbbells) {
            exercises.add(new PlanExercise("Dumbbell Tricep Extension", sets, reps + 4, roundToNearestHalf(baseWeight * 0.15)));
        } else {
            exercises.add(new PlanExercise("Tricep Dips", sets, reps + 4, 0));
        }

        String subtitle = exercises.size() + " exercises · ~"
                + (exercises.size() * 12) + " min · " + getIntensityLabel(goal);

        return new DailyPlan("Today · Push Day", "Chest, Shoulders & Triceps", subtitle, exercises);
    }

    private static double getIntensityPercentForGoal(String goal) {
        if (goal.equals("Get stronger")) return 0.85;
        if (goal.equals("Build muscle mass")) return 0.70;
        if (goal.equals("Get lean and defined")) return 0.60;
        return 0.65; // Reduce bodyweight or default
    }

    private static int getSetsForGoal(String goal) {
        if (goal.equals("Get stronger")) return 4;
        return 3;
    }

    private static int getRepsForGoal(String goal) {
        if (goal.equals("Get stronger")) return 5;
        if (goal.equals("Build muscle mass")) return 10;
        return 12; // Get lean / Reduce bodyweight — higher reps, lower rest
    }

    private static String getIntensityLabel(String goal) {
        if (goal.equals("Get stronger")) return "High intensity";
        if (goal.equals("Build muscle mass")) return "Moderate intensity";
        return "Light-moderate intensity";
    }

    private static double roundToNearestHalf(double value) {
        return Math.round(value * 2) / 2.0;
    }
}