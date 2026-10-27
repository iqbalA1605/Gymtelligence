package com.example.savex.model;

import java.util.ArrayList;
import java.util.List;

public class WorkoutSession {
    public String date;
    public String split;
    public String status; // "none" / "partial" / "full"
    public List<TrackedExercise> exercises = new ArrayList<>();

    public WorkoutSession() {} // required for Firestore
}