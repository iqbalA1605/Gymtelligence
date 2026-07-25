package com.example.savex.model;

import java.util.List;

public class DailyPlan {
    public String dayBadge;      // e.g. "Today · Push Day"
    public String title;         // e.g. "Chest, Shoulders & Triceps"
    public String subtitle;      // e.g. "4 exercises · ~50 min · Moderate intensity"
    public List<PlanExercise> exercises;

    public DailyPlan(String dayBadge, String title, String subtitle, List<PlanExercise> exercises) {
        this.dayBadge = dayBadge;
        this.title = title;
        this.subtitle = subtitle;
        this.exercises = exercises;
    }
}