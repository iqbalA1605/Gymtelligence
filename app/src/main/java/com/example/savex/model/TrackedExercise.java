package com.example.savex.model;

public class TrackedExercise {
    public String name;
    public String bodyPart;
    public int sets;
    public int reps;
    public double weightKg;
    public String status;    // "completed" or "pending"
    public boolean carryOver;

    public TrackedExercise() {} // required for Firestore

    public TrackedExercise(String name, String bodyPart, int sets, int reps, double weightKg, boolean carryOver) {
        this.name = name;
        this.bodyPart = bodyPart;
        this.sets = sets;
        this.reps = reps;
        this.weightKg = weightKg;
        this.status = "pending";
        this.carryOver = carryOver;
    }
}