package com.example.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Verification extends AppCompatActivity {

    private EditText editInstitution, editGmail;
    private RadioGroup radioAffiliation, radioUploadOption;
    private TextView btnChooseImage, btnChooseFile;

    private AppCompatButton btnSubmit;
    private TextView textUploadTitle, textFileName, textAlreadySubmitted;

    private Uri selectedUri = null;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedUri = uri;
                            textFileName.setText("Selected Image: " + uri.getLastPathSegment());
                            textFileName.setVisibility(View.VISIBLE);
                            btnSubmit.setVisibility(View.VISIBLE);
                        }
                    });

    private final ActivityResultLauncher<String> pickFile =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedUri = uri;
                            textFileName.setText("Selected File: " + uri.getLastPathSegment());
                            textFileName.setVisibility(View.VISIBLE);
                            btnSubmit.setVisibility(View.VISIBLE);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        editInstitution = findViewById(R.id.editInstitution);
        editGmail = findViewById(R.id.editGmail);
        radioAffiliation = findViewById(R.id.radioAffiliation);
        radioUploadOption = findViewById(R.id.radioUploadOption);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnChooseFile = findViewById(R.id.btnChooseFile);
        btnSubmit = findViewById(R.id.btnSubmit);
        textUploadTitle = findViewById(R.id.textUploadTitle);
        textFileName = findViewById(R.id.textFileName);
        textAlreadySubmitted = findViewById(R.id.textAlreadySubmitted);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        TextView tvBack = findViewById(R.id.tvBack);
        tvBack.setOnClickListener(v -> finish());

        if (currentUser == null) {
            showCustomToast("Please login first!");
            finish();
            return;
        }

        checkExistingApplication();

        findViewById(R.id.containerTOR).setVisibility(View.GONE);
        findViewById(R.id.containerGmail).setVisibility(View.GONE);
        findViewById(R.id.containerSubmit).setVisibility(View.GONE);

        radioAffiliation.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioYes) {
                editInstitution.setVisibility(View.VISIBLE);
            } else {
                editInstitution.setVisibility(View.GONE);
            }

            findViewById(R.id.containerTOR).setVisibility(View.VISIBLE);
            findViewById(R.id.containerGmail).setVisibility(View.VISIBLE);
            findViewById(R.id.containerSubmit).setVisibility(View.VISIBLE);
        });

        radioUploadOption.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioImage) {
                btnChooseImage.setVisibility(View.VISIBLE);
                btnChooseFile.setVisibility(View.GONE);
            } else {
                btnChooseImage.setVisibility(View.GONE);
                btnChooseFile.setVisibility(View.VISIBLE);
            }
        });

        btnChooseImage.setOnClickListener(v -> pickImage.launch("image/*"));
        btnChooseFile.setOnClickListener(v -> pickFile.launch("application/pdf"));

        btnSubmit.setOnClickListener(v -> {
            if (selectedUri == null) {
                showCustomToast("Please upload your TOR first!");
                return;
            }

            String gmail = editGmail.getText().toString().trim();
            if (gmail.isEmpty() || !gmail.endsWith("@gmail.com")) {
                showCustomToast("Please enter a valid Gmail address!");
                return;
            }

            String institution = editInstitution.getVisibility() == View.VISIBLE
                    ? editInstitution.getText().toString()
                    : "N/A";

            uploadToCloudinary(gmail, institution, selectedUri);
        });
    }

    private void checkExistingApplication() {
        db.collection("applications")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        findViewById(R.id.containerAffiliation).setVisibility(View.GONE);
                        findViewById(R.id.containerTOR).setVisibility(View.GONE);
                        findViewById(R.id.containerGmail).setVisibility(View.GONE);
                        findViewById(R.id.containerSubmit).setVisibility(View.GONE);

                        textAlreadySubmitted.setVisibility(View.VISIBLE);

                        String status = query.getDocuments().get(0).getString("status");
                        if ("approved".equalsIgnoreCase(status)) {
                            textAlreadySubmitted.setText("You are verified");
                            textAlreadySubmitted.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                            db.collection("users")
                                    .document(currentUser.getUid())
                                    .update("verified", true);

                            db.collection("users")
                                    .document(currentUser.getUid())
                                    .update("achievements", com.google.firebase.firestore.FieldValue.arrayUnion("Verified Mushroom Expert"));
                        } else {
                            textAlreadySubmitted.setText("You have already submitted your application. Please wait for admin’s approval. We will notify you via email.");
                            textAlreadySubmitted.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        }
                    }
                });
    }

    private void uploadToCloudinary(String gmail, String institution, Uri fileUri) {
        String mimeType = getContentResolver().getType(fileUri);
        String resourceType = (mimeType != null && mimeType.equals("application/pdf")) ? "raw" : "auto";

        MediaManager.get().upload(fileUri)
                .unsigned("mycoscan")
                .option("resource_type", resourceType)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        showCustomToast("Uploading...");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String fileUrl = (String) resultData.get("secure_url");

                        Map<String, Object> application = new HashMap<>();
                        application.put("userId", currentUser.getUid());
                        application.put("gmail", gmail);
                        application.put("institution", institution);
                        application.put("fileUrl", fileUrl);
                        application.put("status", "pending");
                        application.put("timestamp", System.currentTimeMillis());

                        db.collection("applications")
                                .add(application)
                                .addOnSuccessListener(docRef -> {
                                    showCustomToast("Application submitted successfully!");
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        showCustomToast("Failed to save application: " + e.getMessage()));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        showCustomToast("Upload failed: " + error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.toast_root));

        TextView toastText = layout.findViewById(R.id.toast_text);
        toastText.setText(message);

        Toast toast = new Toast(this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
