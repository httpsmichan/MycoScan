package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private EditText etReason;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        etReason = findViewById(R.id.etReason);
        btnSubmit = findViewById(R.id.btnSubmitReport);

        String postId = getIntent().getStringExtra("postId");

        btnSubmit.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                showCustomToast("Please enter a reason");
                return;
            }

            String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : "anonymous";

            Report report = new Report(postId, userId, System.currentTimeMillis(), reason);

            FirebaseFirestore.getInstance()
                    .collection("reports")
                    .add(report)
                    .addOnSuccessListener(aVoid -> {
                        showCustomToast("Report submitted");

                        Map<String, Object> log = new HashMap<>();
                        log.put("username", FirebaseAuth.getInstance().getCurrentUser() != null
                                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                                : "anonymous");
                        log.put("postId", postId);
                        log.put("datestamp", System.currentTimeMillis());
                        log.put("reason", "user reported a post (postId: " + postId + ")");

                        FirebaseFirestore.getInstance()
                                .collection("logs")
                                .add(log)
                                .addOnSuccessListener(doc ->
                                        showCustomToast("Report logged"))
                                .addOnFailureListener(e ->
                                        showCustomToast("Report saved but log failed"));

                        finish();
                    })
                    .addOnFailureListener(e ->
                            showCustomToast("Failed to submit report"));
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
}
