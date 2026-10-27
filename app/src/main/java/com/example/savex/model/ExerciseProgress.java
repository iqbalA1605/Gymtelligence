package com.example.savex.model;

public class ExerciseProgress {
    public String exerciseName;
    public double lastWeight;
    public int consecutiveCompletions;
    public String lastCompletedDate;

    public ExerciseProgress() {} // required for Firestore
}