package com.example.savex.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // --- Account (filled in later by Step 6 — Register/OTP) ---
    public String email;
    public String fullName;

    // --- Screen 1: About You ---
    public String gender;
    public double heightCm;
    public double weightKg;

    // --- Screen 2: Your Goal ---
    public String mainGoal;
    public String focusAreasCsv; // stored as comma-separated text, e.g. "Chest,Back,Legs"

    // --- Screen 3: Your Level ---
    public String benchmarkExercise;
    public double weightLifted;
    public int repsTillTired;
    public double estimated1RM;

    // --- Screen 4: Your Setup ---
    public int daysPerWeek;
    public String equipmentType; // stored as the enum's name(), e.g. "ALL"

    public long createdAt;
}