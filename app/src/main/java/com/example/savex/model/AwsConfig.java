package com.example.savex.model;

import com.amazonaws.regions.Regions;

public class AwsConfig {
    public static final String COGNITO_POOL_ID = "eu-north-1_kfzo4daeo";
    public static final String COGNITO_CLIENT_ID = "5clhqqoitntkohfscoman993uv";
    public static final String COGNITO_CLIENT_SECRET = null; // leave null unless you generated a client secret
    public static final String IDENTITY_POOL_ID = "eu-north-1:1f13c66a-d9df-47d0-aaff-26c239eafa85";
    public static final Regions AWS_REGION = Regions.EU_NORTH_1;
    public static final String DYNAMODB_TABLE_NAME = "UserProfiles";
}