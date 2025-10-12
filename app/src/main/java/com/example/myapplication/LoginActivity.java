package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private TextView btnSignup, btnForget;
    private AppCompatButton btnLogin;
    private FirebaseAuth mAuth;
    private ListenerRegistration deviceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);

        btnLogin.setOnClickListener(v -> handleLogin());

        btnSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, TermsActivity.class));
        });

        btnForget = findViewById(R.id.btnForget);
        btnForget.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            saveCurrentDevice(currentUser);
            startDeviceWatcher(currentUser);
            navigateToMainActivity();
        }
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showCustomToast("Please fill in all fields");
            return;
        }

        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            db.collection("users")
                                    .document(user.getUid())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String status = documentSnapshot.getString("status");

                                            if ("deactivated".equalsIgnoreCase(status)) {
                                                db.collection("users")
                                                        .document(user.getUid())
                                                        .update("status", "active")
                                                        .addOnSuccessListener(aVoid -> {
                                                            showCustomToast("Welcome back! Your account has been reactivated.");
                                                            saveCurrentDevice(user);
                                                            navigateToMainActivity();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            showCustomToast("Login failed: Could not reactivate account.");
                                                        });
                                            } else {
                                                showCustomToast("Login successful!");
                                                saveCurrentDevice(user);
                                                navigateToMainActivity();
                                            }
                                        } else {
                                            showCustomToast("No account found in database. Please contact support.");
                                            mAuth.signOut();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        showCustomToast("Login failed: Could not fetch account status.");
                                    });
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login failed";

                        if (errorMessage.contains("no user record")) {
                            showCustomToast("No account found with this email. Please sign up first.");
                        } else if (errorMessage.contains("password is invalid")) {
                            showCustomToast("Invalid password. Please try again.");
                        } else if (errorMessage.contains("email address is badly formatted")) {
                            showCustomToast("Invalid email format.");
                        } else if (errorMessage.contains("network")) {
                            showCustomToast("Network error. Please check your connection.");
                        } else {
                            showCustomToast("Login failed: " + errorMessage);
                        }
                    }
                });
    }

    private void saveCurrentDevice(FirebaseUser user) {
        String deviceId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        String model = android.os.Build.MODEL;
        String os = "Android " + android.os.Build.VERSION.RELEASE;

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("deviceId", deviceId);
        deviceInfo.put("model", model);
        deviceInfo.put("os", os);
        deviceInfo.put("lastLogin", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("devices")
                .document(deviceId)
                .set(deviceInfo)
                .addOnSuccessListener(aVoid ->
                        android.util.Log.d("SECURITY", "Device saved: " + model))
                .addOnFailureListener(e ->
                        android.util.Log.e("SECURITY", "Failed to save device", e));
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, TabbedActivity.class);
        startActivity(intent);
        finish();
    }

    private void startDeviceWatcher(FirebaseUser user) {
        String deviceId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        deviceListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("devices")
                .document(deviceId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        android.util.Log.e("SECURITY", "Device watcher failed", e);
                        return;
                    }
                    if (snapshot != null && !snapshot.exists()) {
                        FirebaseAuth.getInstance().signOut();
                        android.util.Log.d("SECURITY", "This device was logged out remotely!");

                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceListener != null) deviceListener.remove();
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
}
