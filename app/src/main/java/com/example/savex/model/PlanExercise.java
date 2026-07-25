package com.example.savex.model;

public class PlanExercise {
    public String name;
    public int sets;
    public int reps;
    public double suggestedWeightKg; // 0 if bodyweight-only

    public PlanExercise(String name, int sets, int reps, double suggestedWeightKg) {
        this.name = name;
        this.sets = sets;
        this.reps = reps;
        this.suggestedWeightKg = suggestedWeightKg;
    }
}