package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPassword";

    private LinearLayout containerPhone, containerNewPassword;
    private EditText etPhoneOrEmail, etOtp, etNewPassword, etConfirmNew;
    private TextView btnSend, btnReset;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String verificationId;
    private String matchedUid;
    private String matchedEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        containerPhone = findViewById(R.id.containerPhone);
        containerNewPassword = findViewById(R.id.containerNewPassword);

        etPhoneOrEmail = findViewById(R.id.etPhoneOrEmail);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNew = findViewById(R.id.etConfirmNew);

        btnSend = findViewById(R.id.btnSend);
        btnReset = findViewById(R.id.btnReset);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnSend.setOnClickListener(v -> sendOtpStep());

        btnReset.setOnClickListener(v -> resetPasswordStep());
    }

    private void sendOtpStep() {
        String phone = etPhoneOrEmail.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            showCustomToast("Please enter phone number");
            return;
        }

        db.collection("users").whereEqualTo("mobile", phone)
                .get().addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        DocumentSnapshot doc = query.getDocuments().get(0);
                        matchedUid = doc.getId();
                        matchedEmail = doc.getString("email");

                        sendOtp(phone);

                        etOtp.setVisibility(View.VISIBLE);

                        btnSend.setText("Verify OTP");
                        btnSend.setOnClickListener(v -> verifyOtpStep());

                        etPhoneOrEmail.setEnabled(false);

                    } else {
                        showCustomToast("No account found with this mobile number.");
                    }
                }).addOnFailureListener(e -> {
                    showCustomToast("Error checking mobile number: " + e.getMessage());
                });
    }

    private void sendOtp(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        showCustomToast("OTP auto-retrieved");
                        signInWithPhoneCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        showCustomToast("Verification failed: " + e.getMessage());
                    }

                    @Override
                    public void onCodeSent(String verId, PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = verId;
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyOtpStep() {
        String code = etOtp.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            showCustomToast("Please enter OTP");
            return;
        }

        if (verificationId != null) {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithPhoneCredential(credential);
        } else {
            showCustomToast("OTP not requested properly. Try again.");
        }
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showCustomToast("Phone verified! Please set a new password.");

                containerNewPassword.setVisibility(View.VISIBLE);
                btnReset.setVisibility(View.VISIBLE);

                etOtp.setEnabled(false);
                btnSend.setEnabled(false);

            } else {
                showCustomToast("Invalid OTP. Try again.");}
        });
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.custom_toast, null);

        TextView toastText = layout.findViewById(R.id.toast_text);
        toastText.setText(message);

        Toast toast = new Toast(this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }


    private void resetPasswordStep() {
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmNew.getText().toString().trim();

        if (TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
            showCustomToast("Please enter both password fields");
            return;
        }

        if (newPass.length() < 6) {
            showCustomToast("Password must be at least 6 characters");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showCustomToast("Passwords do not match");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && matchedEmail != null) {
            user.updatePassword(newPass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    showCustomToast("Password updated successfully!");

                    mAuth.signOut();

                    startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                } else {
                    String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    showCustomToast("Failed to update password: " + error);
                }
            });
        } else {
            showCustomToast("Session expired. Please verify phone again.");
            finish();
        }
    }
}
