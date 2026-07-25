package com.example.savex;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;

public class OtpVerifyActivity extends AppCompatActivity {

    private EditText etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6;
    private Button btnVerifyOtp;
    private ProgressBar progressBarOtp;
    private TextView tvOtpError, tvOtpSubtitle, tvResendOtp;

    private String email;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verify);

        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        etOtp5 = findViewById(R.id.etOtp5);
        etOtp6 = findViewById(R.id.etOtp6);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        progressBarOtp = findViewById(R.id.progressBarOtp);
        tvOtpError = findViewById(R.id.tvOtpError);
        tvOtpSubtitle = findViewById(R.id.tvOtpSubtitle);
        tvResendOtp = findViewById(R.id.tvResendOtp);

        if (email != null) {
            tvOtpSubtitle.setText("Enter the 6-digit code sent to " + email);
        }

        setupOtpAutoAdvance();

        btnVerifyOtp.setOnClickListener(v -> attemptVerify());

        tvResendOtp.setOnClickListener(v -> resendOtp());
    }

    private void setupOtpAutoAdvance() {
        final EditText[] boxes = {etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6};

        for (int i = 0; i < boxes.length; i++) {
            final int index = i;

            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < boxes.length - 1) {
                        boxes[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            boxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && boxes[index].getText().toString().isEmpty()
                        && index > 0) {
                    boxes[index - 1].requestFocus();
                    boxes[index - 1].setText("");
                    return true;
                }
                return false;
            });
        }
    }

    private String getEnteredOtp() {
        return etOtp1.getText().toString()
                + etOtp2.getText().toString()
                + etOtp3.getText().toString()
                + etOtp4.getText().toString()
                + etOtp5.getText().toString()
                + etOtp6.getText().toString();
    }

    private void attemptVerify() {
        String otp = getEnteredOtp();

        if (otp.length() != 6) {
            showError("Please enter the full 6-digit code");
            return;
        }
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError("Missing sign-up details, please sign up again");
            return;
        }

        hideError();
        setLoading(true);

        AWSMobileClient.getInstance().confirmSignUp(
                email,
                otp,
                new com.amazonaws.mobile.client.Callback<com.amazonaws.mobile.client.results.SignUpResult>() {
                    @Override
                    public void onResult(com.amazonaws.mobile.client.results.SignUpResult result) {
                        // Account is confirmed now, but NOT signed in yet — do that next.
                        signInAfterConfirmation();
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showError(e.getMessage() != null ? e.getMessage() : "Invalid code. Try again.");
                        });
                    }
                }
        );
    }

    /** Confirming the OTP only verifies the account — it doesn't create a session, so we sign in explicitly here. */
    private void signInAfterConfirmation() {
        AWSMobileClient.getInstance().signIn(
                email,
                password,
                null,
                new com.amazonaws.mobile.client.Callback<com.amazonaws.mobile.client.results.SignInResult>() {
                    @Override
                    public void onResult(com.amazonaws.mobile.client.results.SignInResult signInResult) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(OtpVerifyActivity.this, "Account verified!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(OtpVerifyActivity.this, AboutYouActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showError("Verified, but sign-in failed: " + (e.getMessage() != null ? e.getMessage() : "unknown error"));
                        });
                    }
                }
        );
    }

    private void resendOtp() {
        if (TextUtils.isEmpty(email)) return;

        AWSMobileClient.getInstance().resendSignUp(
                email,
                new com.amazonaws.mobile.client.Callback<com.amazonaws.mobile.client.results.SignUpResult>() {
                    @Override
                    public void onResult(com.amazonaws.mobile.client.results.SignUpResult result) {
                        runOnUiThread(() -> Toast.makeText(OtpVerifyActivity.this, "Code resent", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> Toast.makeText(OtpVerifyActivity.this, "Could not resend code", Toast.LENGTH_SHORT).show());
                    }
                }
        );
    }

    private void setLoading(boolean loading) {
        btnVerifyOtp.setEnabled(!loading);
        progressBarOtp.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        tvOtpError.setText(message);
        tvOtpError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvOtpError.setVisibility(View.GONE);
    }
}