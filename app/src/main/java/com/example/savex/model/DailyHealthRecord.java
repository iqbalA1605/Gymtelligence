package com.example.savex.model;

public class DailyHealthRecord {
    public String date;       // "yyyy-MM-dd"
    public int steps;
    public double sleepHours;
    public int heartRateBpm;
    public Integer stepTarget; // the target that applied to THIS date, set the day before; null if none

    public DailyHealthRecord() {
        // required no-arg constructor for Firestore
    }
}