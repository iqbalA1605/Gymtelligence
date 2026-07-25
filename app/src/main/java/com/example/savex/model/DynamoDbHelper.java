package com.example.savex.model;

import android.content.Context;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class DynamoDbHelper {

    public interface ProfileCallback {
        void onSuccess(Map<String, AttributeValue> item); // null if no profile exists yet
        void onError(Exception e);
    }

    public interface WriteCallback {
        void onSuccess();
        void onError(Exception e);
    }

    /** The stable per-user ID used as the DynamoDB partition key — tied to the signed-in session. */
    public static String getCurrentUserId(Context context) {
        return AWSMobileClient.getInstance().getIdentityId();
    }

    private static AmazonDynamoDBClient buildClient() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(AWSMobileClient.getInstance());
        client.setRegion(com.amazonaws.regions.Region.getRegion(AwsConfig.AWS_REGION));
        return client;
    }

    public static void saveUserProfile(Context context, UserProfile profile, WriteCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AmazonDynamoDBClient client = buildClient();

                Map<String, AttributeValue> item = new HashMap<>();
                item.put("userId", new AttributeValue().withS(getCurrentUserId(context)));
                item.put("gender", new AttributeValue().withS(nullSafe(profile.gender)));
                item.put("heightCm", new AttributeValue().withN(String.valueOf(profile.heightCm)));
                item.put("weightKg", new AttributeValue().withN(String.valueOf(profile.weightKg)));
                item.put("mainGoal", new AttributeValue().withS(nullSafe(profile.mainGoal)));
                item.put("focusAreas", new AttributeValue().withS(nullSafe(profile.focusAreasCsv)));
                item.put("benchmarkExercise", new AttributeValue().withS(nullSafe(profile.benchmarkExercise)));
                item.put("weightLifted", new AttributeValue().withN(String.valueOf(profile.weightLifted)));
                item.put("repsTillTired", new AttributeValue().withN(String.valueOf(profile.repsTillTired)));
                item.put("estimated1RM", new AttributeValue().withN(String.valueOf(profile.estimated1RM)));
                item.put("daysPerWeek", new AttributeValue().withN(String.valueOf(profile.daysPerWeek)));
                item.put("equipmentType", new AttributeValue().withS(nullSafe(profile.equipmentType)));
                item.put("createdAt", new AttributeValue().withN(String.valueOf(System.currentTimeMillis())));

                client.putItem(new PutItemRequest().withTableName(AwsConfig.DYNAMODB_TABLE_NAME).withItem(item));
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public static void getUserProfile(Context context, ProfileCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AmazonDynamoDBClient client = buildClient();

                Map<String, AttributeValue> key = new HashMap<>();
                key.put("userId", new AttributeValue().withS(getCurrentUserId(context)));

                GetItemResult result = client.getItem(new GetItemRequest()
                        .withTableName(AwsConfig.DYNAMODB_TABLE_NAME)
                        .withKey(key));

                callback.onSuccess(result.getItem()); // null if the profile doesn't exist yet
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}