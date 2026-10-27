package com.example.savex;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.client.results.SignInState;
import com.example.savex.model.AuthErrorMapper;

public class SignInActivity extends AppCompatActivity {

    private EditText etSignInEmail, etSignInPassword;
    private ImageView ivToggleSignInPassword;
    private Button btnSignIn;
    private ProgressBar progressBarSignIn;
    private TextView tvSignInError, tvGoToSignUp;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        etSignInEmail = findViewById(R.id.etSignInEmail);
        etSignInPassword = findViewById(R.id.etSignInPassword);
        ivToggleSignInPassword = findViewById(R.id.ivToggleSignInPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        progressBarSignIn = findViewById(R.id.progressBarSignIn);
        tvSignInError = findViewById(R.id.tvSignInError);
        tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        String prefilledEmail = getIntent().getStringExtra("email");
        if (prefilledEmail != null) {
            etSignInEmail.setText(prefilledEmail);
        }

        ivToggleSignInPassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            togglePasswordVisibility();
        });

        btnSignIn.setOnClickListener(v -> attemptSignIn());

        tvGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
            finish();
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etSignInPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivToggleSignInPassword.setImageResource(R.drawable.ic_eye_show);
        } else {
            etSignInPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivToggleSignInPassword.setImageResource(R.drawable.ic_eye_hide);
        }
        etSignInPassword.setSelection(etSignInPassword.getText().length());
    }

    private void attemptSignIn() {
        String email = etSignInEmail.getText().toString().trim();
        String password = etSignInPassword.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password");
            return;
        }

        hideError();
        setLoading(true);

        AWSMobileClient.getInstance().signIn(
                email,
                password,
                null,
                new com.amazonaws.mobile.client.Callback<SignInResult>() {
                    @Override
                    public void onResult(SignInResult result) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            if (result.getSignInState() == SignInState.DONE) {
                                Intent intent = new Intent(SignInActivity.this, HomeActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                showError("Additional verification is required to sign in.");
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("SIGNIN_FAILURE", "Sign in failed", e);
                        runOnUiThread(() -> {
                            setLoading(false);
                            showError(AuthErrorMapper.toUserMessage(e));
                        });
                    }
                }
        );
    }

    private void setLoading(boolean loading) {
        btnSignIn.setEnabled(!loading);
        progressBarSignIn.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        tvSignInError.setText(message);
        tvSignInError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvSignInError.setVisibility(View.GONE);
    }
}