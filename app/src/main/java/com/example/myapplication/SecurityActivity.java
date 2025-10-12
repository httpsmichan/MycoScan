package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SecurityActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private AppCompatButton btnChangePassword;
    private TextView btnLogoutAllDevices, btnDeactivate, btnDeleteAccount;
    private LinearLayout containerDevices;

    private FirebaseUser user;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogoutAllDevices = findViewById(R.id.btnLogoutAllDevices);
        btnDeactivate = findViewById(R.id.btnDeactivate);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        containerDevices = findViewById(R.id.containerDevices);

        TextView tvBack = findViewById(R.id.tvBack);
        tvBack.setOnClickListener(v -> finish());

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (user != null) {
            saveCurrentDevice();
        }

        btnChangePassword.setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (currentPassword.isEmpty()) {
                showCustomToast("Enter current password");
                return;
            }

            if (newPassword.isEmpty() || newPassword.length() < 6) {
                showCustomToast("Password must be at least 6 characters");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showCustomToast("Passwords do not match");
                return;
            }

            if (user != null && user.getEmail() != null) {
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
                user.reauthenticate(credential)
                        .addOnSuccessListener(aVoid -> {
                            user.updatePassword(newPassword)
                                    .addOnSuccessListener(aVoid2 -> {
                                        showCustomToast("Password updated!");

                                        Map<String, Object> log = new HashMap<>();
                                        log.put("username", user.getEmail());
                                        log.put("datestamp", System.currentTimeMillis());
                                        log.put("reason", "user changed password");

                                        db.collection("logs").add(log);
                                    })
                                    .addOnFailureListener(e -> showCustomToast("Update failed: " + e.getMessage()));
                        })
                        .addOnFailureListener(e -> showCustomToast("Re-authentication failed: " + e.getMessage()));
            }
        });

        loadDevices();

        btnLogoutAllDevices.setOnClickListener(v -> logoutAllDevices());

        btnDeactivate.setOnClickListener(v -> {
            if (user == null) return;

            db.collection("users")
                    .document(user.getUid())
                    .update("status", "deactivated")
                    .addOnSuccessListener(aVoid -> {
                        showCustomToast("Account deactivated (temporary)");

                        FirebaseAuth.getInstance().signOut();

                        Intent intent = new Intent(SecurityActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> showCustomToast("Failed to deactivate: " + e.getMessage()));
        });

        btnDeleteAccount.setOnClickListener(v -> {
            if (user == null) return;

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        user.delete()
                                .addOnSuccessListener(aVoid -> {
                                    db.collection("users").document(user.getUid()).delete();
                                    showCustomToast("Account deleted permanently");
                                    FirebaseAuth.getInstance().signOut();
                                    finish();
                                })
                                .addOnFailureListener(e -> showCustomToast("Failed to delete: " + e.getMessage()));
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void saveCurrentDevice() {
        if (user == null) return;

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String model = android.os.Build.MODEL;
        String os = "Android " + android.os.Build.VERSION.RELEASE;

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("deviceId", deviceId);
        deviceInfo.put("model", model);
        deviceInfo.put("os", os);
        deviceInfo.put("lastLogin", System.currentTimeMillis());

        db.collection("users")
                .document(user.getUid())
                .collection("devices")
                .document(deviceId)
                .set(deviceInfo);
    }

    private void loadDevices() {
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("devices")
                .get()
                .addOnSuccessListener(query -> {
                    containerDevices.removeAllViews();

                    if (query.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("(No devices found)");
                        tv.setTextSize(12);
                        containerDevices.addView(tv);
                    } else {
                        for (var doc : query.getDocuments()) {
                            String model = doc.getString("model");
                            String os = doc.getString("os");
                            Long lastLogin = doc.getLong("lastLogin");

                            LinearLayout deviceContainer = new LinearLayout(this);
                            deviceContainer.setOrientation(LinearLayout.VERTICAL);
                            deviceContainer.setBackgroundResource(R.drawable.dialog_background);

                            TextView tv = new TextView(this);
                            tv.setText(model + " | " + os + " | Last login: " +
                                    DateFormat.format("yyyy-MM-dd HH:mm", lastLogin));
                            tv.setTextSize(14);

                            deviceContainer.addView(tv);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(0, 15, 0, 15);
                            containerDevices.addView(deviceContainer, params);
                        }
                    }
                })
                .addOnFailureListener(e -> showCustomToast("Error loading devices"));
    }

    private void logoutAllDevices() {
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(user.getUid())
                .collection("devices")
                .get()
                .addOnSuccessListener(query -> {
                    for (var doc : query.getDocuments()) {
                        doc.getReference().delete();
                    }

                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(SecurityActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();

                    showCustomToast("Logged out from all devices!");
                })
                .addOnFailureListener(e -> showCustomToast("Failed to log out all devices: " + e.getMessage()));
    }

    private void showCustomToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
