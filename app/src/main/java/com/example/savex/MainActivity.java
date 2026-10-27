package com.example.savex;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.example.savex.databinding.ActivityMainBinding;
import com.example.savex.model.FirestoreHelper;
import com.example.savex.model.UserProfile;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails userStateDetails) {
                if (userStateDetails.getUserState() == UserState.SIGNED_IN) {
                    checkProfileAndRoute();
                } else {
                    routeTo(SignUpActivity.class);
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> routeTo(SignUpActivity.class));
            }
        });
    }

    private void checkProfileAndRoute() {
        FirestoreHelper.getUserProfile(getApplicationContext(), new FirestoreHelper.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> routeTo(profile != null ? HomeActivity.class : AboutYouActivity.class));
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> routeTo(AboutYouActivity.class));
            }
        });
    }

    private void routeTo(Class<?> destination) {
        startActivity(new Intent(MainActivity.this, destination));
        finish();
    }
}