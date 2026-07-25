plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.savex"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.savex"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("com.amazonaws:aws-android-sdk-core:2.81.1")
    implementation("com.amazonaws:aws-android-sdk-cognitoidentityprovider:2.81.1")
    implementation("com.amazonaws:aws-android-sdk-ddb:2.81.1")
    implementation("com.amazonaws:aws-android-sdk-mobile-client:2.81.1")
}