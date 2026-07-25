package com.example.savex;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.example.savex.model.AuthErrorMapper;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private ImageView ivTogglePassword, ivToggleConfirmPassword;
    private Button btnGetOtp;
    private ProgressBar progressBar;
    private TextView tvError, tvGoToSignIn;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnGetOtp = findViewById(R.id.btnGetOtp);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        tvGoToSignIn = findViewById(R.id.tvGoToSignIn);

        AWSMobileClient.getInstance().initialize(getApplicationContext(), new com.amazonaws.mobile.client.Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                // Ready — signUp() is now safe to call once the user taps "Get OTP"
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> showError("Could not connect to authentication service"));
            }
        });

        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            togglePasswordVisibility(etPassword, ivTogglePassword, isPasswordVisible);
        });

        ivToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword, isConfirmPasswordVisible);
        });

        btnGetOtp.setOnClickListener(v -> attemptSignUp());

        tvGoToSignIn.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
            finish();
        });
    }

    private void togglePasswordVisibility(EditText field, ImageView icon, boolean visible) {
        if (visible) {
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            icon.setImageResource(R.drawable.ic_eye_show);
        } else {
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            icon.setImageResource(R.drawable.ic_eye_hide);
        }
        field.setSelection(field.getText().length());
    }

    private void attemptSignUp() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(name)) {
            showError("Please enter your name");
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 8) {
            showError("Password must be at least 8 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        hideError();
        setLoading(true);

        Map<String, String> userAttributes = new HashMap<>();
        userAttributes.put("email", email);
        userAttributes.put("name", name);

        AWSMobileClient.getInstance().signUp(
                email,
                password,
                userAttributes,
                null,
                new com.amazonaws.mobile.client.Callback<SignUpResult>() {
                    @Override
                    public void onResult(SignUpResult result) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Intent intent = new Intent(SignUpActivity.this, OtpVerifyActivity.class);
                            intent.putExtra("email", email);
                            intent.putExtra("password", password); // needed so OtpVerifyActivity can sign in after confirming
                            startActivity(intent);
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showError(AuthErrorMapper.toUserMessage(e));
                        });
                    }
                }
        );
    }

    private void setLoading(boolean loading) {
        btnGetOtp.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }
}