package com.example.savex.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HealthTrendAnalyzer {

    public static class StepInsight {
        public String achievementMessage;
        public Integer todayTarget;
    }

    public static String todayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
    }

    public static String yesterdayDateString() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    public static StepInsight analyzeSteps(List<DailyHealthRecord> history) {
        StepInsight insight = new StepInsight();
        if (history == null || history.isEmpty()) return insight;

        String yesterday = yesterdayDateString();
        DailyHealthRecord yesterdayRecord = null;
        for (DailyHealthRecord r : history) {
            if (yesterday.equals(r.date)) { yesterdayRecord = r; break; }
        }

        if (yesterdayRecord != null && yesterdayRecord.stepTarget != null) {
            if (yesterdayRecord.steps >= yesterdayRecord.stepTarget) {
                insight.achievementMessage = "Yesterday you hit your target of " + yesterdayRecord.stepTarget + " steps!";
            } else {
                insight.achievementMessage = "Yesterday's target was " + yesterdayRecord.stepTarget
                        + " steps — you reached " + yesterdayRecord.steps + ", so it wasn't achieved.";
            }
        }

        // Relaxed: real daily step counts vary a lot; only need 3+ days of any history, no tight consistency gate.
        if (history.size() >= 3) {
            double sum = 0;
            for (int i = 0; i < 3; i++) sum += history.get(i).steps;
            double avg = sum / 3.0;
            if (avg > 0) {
                insight.todayTarget = (int) Math.round(avg * 1.05);
            }
        }

        return insight;
    }

    public static String analyzeSleep(double todaySleepHours, List<DailyHealthRecord> weekHistory) {
        if (weekHistory == null || weekHistory.isEmpty()) {
            return "Not enough sleep history yet to compare.";
        }
        double sum = 0; int count = 0;
        for (DailyHealthRecord r : weekHistory) {
            if (r.sleepHours > 0) { sum += r.sleepHours; count++; }
        }
        if (count == 0) return "Not enough sleep history yet to compare.";

        double average = sum / count;
        if (todaySleepHours <= 0) {
            return String.format(Locale.US, "Your weekly average is %.1f hrs — no sleep data for today yet.", average);
        }
        double diff = todaySleepHours - average;
        if (diff > 0.5) return String.format(Locale.US, "You slept %.1f hrs — above your %.1f hr weekly average.", todaySleepHours, average);
        if (diff < -0.5) return String.format(Locale.US, "You slept %.1f hrs — below your %.1f hr weekly average.", todaySleepHours, average);
        return String.format(Locale.US, "You slept %.1f hrs — about the same as your %.1f hr weekly average.", todaySleepHours, average);
    }
}