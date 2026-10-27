package com.example.savex.model;

import android.content.Context;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreHelper {

    private static final String COLLECTION_NAME = "UserProfiles";

    public interface ProfileCallback {
        void onSuccess(UserProfile profile);
        void onError(Exception e);
    }

    public interface WriteCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // TEMPORARY: using a local device ID instead of AWSMobileClient.getInstance().getUsername()
    // while auth is bypassed. Revert to Cognito username once auth is re-enabled.
    public static String getCurrentUserId(Context context) {
        return com.amazonaws.mobile.client.AWSMobileClient.getInstance().getUsername();
    }

    public static void saveUserProfile(Context context, UserProfile profile, WriteCallback callback) {
        String userId = getCurrentUserId(context);

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_NAME)
                .document(userId)
                .set(profile)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public static void getUserProfile(Context context, ProfileCallback callback) {
        String userId = getCurrentUserId(context);

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_NAME)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        callback.onSuccess(snapshot.toObject(UserProfile.class));
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    private static final String HISTORY_SUBCOLLECTION = "HealthHistory";

    public interface HistoryCallback {
        void onSuccess(java.util.List<DailyHealthRecord> records); // most recent first
        void onError(Exception e);
    }

    public static void saveDailyHealthRecord(Context context, DailyHealthRecord record, WriteCallback callback) {
        String userId = getCurrentUserId(context);
        if (userId == null) {
            callback.onError(new IllegalStateException("No signed-in user found"));
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_NAME)
                .document(userId)
                .collection(HISTORY_SUBCOLLECTION)
                .document(record.date)
                .set(record, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public static void getRecentHealthHistory(Context context, int days, HistoryCallback callback) {
        String userId = getCurrentUserId(context);
        if (userId == null) {
            callback.onError(new IllegalStateException("No signed-in user found"));
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_NAME)
                .document(userId)
                .collection(HISTORY_SUBCOLLECTION)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(days)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.List<DailyHealthRecord> records = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        DailyHealthRecord record = doc.toObject(DailyHealthRecord.class);
                        if (record != null) records.add(record);
                    }
                    callback.onSuccess(records);
                })
                .addOnFailureListener(callback::onError);
    }

    private static final String WORKOUT_HISTORY_SUBCOLLECTION = "WorkoutHistory";
    private static final String EXERCISE_PROGRESS_SUBCOLLECTION = "ExerciseProgress";

    public interface WorkoutSessionCallback {
        void onSuccess(WorkoutSession session); // null if none exists for that date
        void onError(Exception e);
    }

    public interface WorkoutSessionListCallback {
        void onSuccess(java.util.List<WorkoutSession> sessions); // most recent first
        void onError(Exception e);
    }

    public interface ExerciseProgressMapCallback {
        void onSuccess(java.util.Map<String, ExerciseProgress> progressByName);
        void onError(Exception e);
    }

    public static void saveWorkoutSession(Context context, WorkoutSession session, WriteCallback callback) {
        String userId = getCurrentUserId(context);
        if (userId == null) { callback.onError(new IllegalStateException("No signed-in user found")); return; }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_NAME).document(userId)
                .collection(WORKOUT_HISTORY_SUBCOLLECTION).document(session.date)
                .set(session)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public static void getWorkoutSession(Context context, String date, WorkoutSessionCallback callback) {
        String userId = getCurrentUserId(context);
        if (userId == null) { callback.onError(new IllegalStateException("No signed-in user found")); return; }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_NAME).document(userId)
                .collection(WORKOUT_HISTORY_SUBCOLLECTION).document(date)
                .get()
                .addOnSuccessListener(snap -> callback.onSuccess(snap.exists() ? snap.toObject(WorkoutSession.class) : null))
                .addOnFailureListener(callback::onError);
    }

    public static void getRecentWorkoutSessions(Context context, int days, WorkoutSessionListCallback callback) {
        String userId = getCurrentUserId(context);
        if (userId == null) { callback.onError(new IllegalStateException("No signed-in user found")); return; }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_NAME).document(userId)
                .collection(WORKOUT_HISTORY_SUBCOLLECTION)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(days)
                .get()
                .addOnSuccessListener(qs -> {
                    java.util.List<WorkoutSession> list = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        WorkoutSession s = doc.toObject(WorkoutSession.class);
                        if (s != null) list.add(s);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onError);
    }

    public static void getAllExerciseProgress(Context context, ExerciseProgressMapCallback callback) {
        String userId = getCurrentUserId(context);
        if (userId == null) { callback.onError(new IllegalStateException("No signed-in user found")); return; }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_NAME).document(userId)
                .collection(EXERCISE_PROGRESS_SUBCOLLECTION)
                .get()
                .addOnSuccessListener(qs -> {
                    java.util.Map<String, ExerciseProgress> map = new java.util.HashMap<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : qs.getDocuments()) {
                        ExerciseProgress p = doc.toObject(ExerciseProgress.class);
                        if (p != null) map.put(p.exerciseName, p);
                    }
                    callback.onSuccess(map);
                })
                .addOnFailureListener(callback::onError);
    }

    public static void saveExerciseProgress(Context context, ExerciseProgress progress, WriteCallback callback) {
        String userId = getCurrentUserId(context);
        if (userId == null) { callback.onError(new IllegalStateException("No signed-in user found")); return; }

        FirebaseFirestore.getInstance()
                .collection(COLLECTION_NAME).document(userId)
                .collection(EXERCISE_PROGRESS_SUBCOLLECTION).document(progress.exerciseName)
                .set(progress)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
}