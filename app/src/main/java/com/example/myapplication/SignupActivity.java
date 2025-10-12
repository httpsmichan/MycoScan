package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText etUsername, etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnCreateAccount, btnGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etUsername = findViewById(R.id.etUsername);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        btnCreateAccount.setOnClickListener(v -> handleCreateAccount());

        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void handleCreateAccount() {
        String username = etUsername.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showCustomToast("Please fill in all fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showCustomToast("Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            showCustomToast("Password should be at least 6 characters");
            return;
        }

        final String capitalizedFullName = capitalizeFullName(fullName);

        Query query = db.collection("users").whereEqualTo("username", username);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                showCustomToast("Username already taken");
            } else {
                createUserAccount(username, capitalizedFullName, email, password);
            }
        });
    }

    private String capitalizeFullName(String fullName) {
        String[] words = fullName.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase(Locale.ROOT))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void createUserAccount(String username, String fullName, String email, String password) {
        btnCreateAccount.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnCreateAccount.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserDataToFirestore(user.getUid(), username, fullName, email);

                            Map<String, Object> log = new HashMap<>();
                            log.put("username", username);
                            log.put("datestamp", System.currentTimeMillis());
                            log.put("reason", "new account was made");
                            FirebaseFirestore.getInstance().collection("logs").add(log);

                            startActivity(new Intent(SignupActivity.this, TabbedActivity.class));
                            finish();
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        showCustomToast("Registration failed: " + errorMessage);
                    }
                });
    }

    private void saveUserDataToFirestore(String userId, String username, String fullName, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("fullName", fullName);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());
        user.put("following", 0);
        user.put("followers", 0);

        db.collection("users").document(userId)
                .set(user)
                .addOnFailureListener(e -> showCustomToast("Failed to save user data"));
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
