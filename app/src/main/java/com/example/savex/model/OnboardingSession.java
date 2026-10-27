package com.example.savex.model;

public class OnboardingSession {

    public enum EquipmentType {
        ALL, DUMBBELLS, BODYWEIGHT, MACHINES
    }

    private static OnboardingSession instance;

    private String fullName;

    // --- Screen 1: About You ---
    private String gender;          // "Male" / "Female"
    private double heightCm;
    private double weightKg;

    // --- Screen 2: Your Goal ---
    private String mainGoal;        // e.g. "Get stronger"
    private java.util.List<String> focusAreas = new java.util.ArrayList<>();

    // --- Screen 3: Your Level ---
    private String benchmarkExercise;   // e.g. "Bench Press"
    private double weightLifted;
    private int repsTillTired;
    private double estimated1RM;

    // --- Screen 4: Your Setup ---
    private int daysPerWeek;
    private EquipmentType equipmentType;

    private OnboardingSession() {
    }

    public static synchronized OnboardingSession getInstance() {
        if (instance == null) {
            instance = new OnboardingSession();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    // --- About You ---
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    // --- Your Goal ---
    public String getMainGoal() { return mainGoal; }
    public void setMainGoal(String mainGoal) { this.mainGoal = mainGoal; }

    public java.util.List<String> getFocusAreas() { return focusAreas; }
    public void setFocusAreas(java.util.List<String> focusAreas) { this.focusAreas = focusAreas; }

    // --- Your Level ---
    public String getBenchmarkExercise() { return benchmarkExercise; }
    public void setBenchmarkExercise(String benchmarkExercise) { this.benchmarkExercise = benchmarkExercise; }

    public double getWeightLifted() { return weightLifted; }
    public void setWeightLifted(double weightLifted) { this.weightLifted = weightLifted; }

    public int getRepsTillTired() { return repsTillTired; }
    public void setRepsTillTired(int repsTillTired) { this.repsTillTired = repsTillTired; }

    public double getEstimated1RM() { return estimated1RM; }
    public void setEstimated1RM(double estimated1RM) { this.estimated1RM = estimated1RM; }

    // --- Your Setup ---
    public int getDaysPerWeek() { return daysPerWeek; }
    public void setDaysPerWeek(int daysPerWeek) { this.daysPerWeek = daysPerWeek; }

    public EquipmentType getEquipmentType() { return equipmentType; }
    public void setEquipmentType(EquipmentType equipmentType) { this.equipmentType = equipmentType; }
}