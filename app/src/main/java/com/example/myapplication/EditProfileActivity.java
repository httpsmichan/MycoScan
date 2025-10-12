package com.example.myapplication;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.android.callback.ErrorInfo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;

    private EditText etUsername, etFullName, etBio, etLocation, etMobile ;
    private TextView btnChangePhoto, btnAddWebsite, btnPassword;
    private ImageView ivProfileImage;
    private AppCompatButton btnSaveAll;
    private LinearLayout llWebsitesContainer;

    private String oldProfileUrl;
    private Uri imageUri;
    private Map<String, String> originalData = new HashMap<>();
    private List<String> originalSocials = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        TextView tvBack = findViewById(R.id.tvBack);
        tvBack.setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) finish();
        userId = currentUser.getUid();

        etUsername = findViewById(R.id.etUsername);
        etFullName = findViewById(R.id.etFullName);
        etBio = findViewById(R.id.etBio);
        etLocation = findViewById(R.id.etLocation);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        llWebsitesContainer = findViewById(R.id.llWebsitesContainer);
        btnAddWebsite = findViewById(R.id.btnAddWebsite);
        btnSaveAll = findViewById(R.id.btnSaveAll);
        btnPassword = findViewById(R.id.btnPassword);
        etMobile = findViewById(R.id.etMobile);
        setupEditOnTouch(etMobile);

        etMobile.setText("+63");

        etMobile.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etMobile.getText().toString().isEmpty()) {
                etMobile.setText("+63");
                etMobile.setSelection(etMobile.getText().length());
            }
        });

        etMobile.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String text = s.toString();
                if (!text.startsWith("+63")) {
                    etMobile.setText("+63");
                    etMobile.setSelection(etMobile.getText().length());
                }
            }
        });

        btnPassword.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfileActivity.this, SecurityActivity.class);
            startActivity(intent);
        });

        loadUserData();

        setupEditOnTouch(etUsername);
        setupEditOnTouch(etFullName);
        setupEditOnTouch(etBio);
        setupEditOnTouch(etLocation);

        btnAddWebsite.setOnClickListener(v -> addSocialField(null));
        btnSaveAll.setOnClickListener(v -> saveChanges());

        btnChangePhoto.setOnClickListener(v -> openFileChooser());
    }

    private void loadUserData() {
        db.collection("users").document(userId).get().addOnSuccessListener(document -> {
            if (!document.exists()) return;

            oldProfileUrl = document.getString("profilePhoto");
            if (oldProfileUrl != null && !oldProfileUrl.isEmpty()) {
                Glide.with(this).load(oldProfileUrl).circleCrop().into(ivProfileImage);
            }

            etUsername.setText(document.getString("username"));
            etFullName.setText(document.getString("fullName"));
            etBio.setText(document.getString("bio"));
            etLocation.setText(document.getString("location"));
            etMobile.setText(document.getString("mobile"));

            originalData.put("username", etUsername.getText().toString());
            originalData.put("fullName", etFullName.getText().toString());
            originalData.put("bio", etBio.getText().toString());
            originalData.put("location", etLocation.getText().toString());
            originalData.put("mobile", etMobile.getText().toString());

            List<String> socials = (List<String>) document.get("socials");
            if (socials != null && !socials.isEmpty()) {
                for (String s : socials) addSocialField(s);
                originalSocials = new ArrayList<>(socials);
            } else {
                addSocialField(null);
                originalSocials = new ArrayList<>();
            }

            String mobile = document.getString("mobile");
            if (mobile != null && !mobile.isEmpty()) {
                etMobile.setText(mobile);
            } else {
                etMobile.setText("+63");
            }
            originalData.put("mobile", etMobile.getText().toString());

        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupEditOnTouch(EditText editText) {
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setCursorVisible(false);
        editText.setClickable(true);

        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                editText.setFocusableInTouchMode(true);
                editText.setFocusable(true);
                editText.setCursorVisible(true);
                editText.requestFocus();
            }
            return false;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addSocialField(String url) {
        LinearLayout rowLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.item_social, llWebsitesContainer, false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        rowLayout.setLayoutParams(params);

        EditText etSocial = rowLayout.findViewById(R.id.etSocial);
        etSocial.setText(url != null ? url : "");
        etSocial.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                etSocial.setFocusableInTouchMode(true);
                etSocial.setFocusable(true);
                etSocial.setCursorVisible(true);
                etSocial.requestFocus();
            }
            return false;
        });
        ImageButton btnRemove = rowLayout.findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(v -> llWebsitesContainer.removeView(rowLayout));
        llWebsitesContainer.addView(rowLayout);
    }

    private void saveChanges() {
        Map<String, Object> updates = new HashMap<>();
        checkAndPutUpdate("username", etUsername.getText().toString(), updates);
        checkAndPutUpdate("fullName", etFullName.getText().toString(), updates);
        checkAndPutUpdate("bio", etBio.getText().toString(), updates);
        checkAndPutUpdate("location", etLocation.getText().toString(), updates);
        checkAndPutUpdate("mobile", etMobile.getText().toString(), updates);

        List<String> socialsList = new ArrayList<>();
        for (int i = 0; i < llWebsitesContainer.getChildCount(); i++) {
            LinearLayout rowLayout = (LinearLayout) llWebsitesContainer.getChildAt(i);
            EditText et = rowLayout.findViewById(R.id.etSocial);
            String url = et.getText().toString().trim();
            if (!url.isEmpty()) socialsList.add(url);
        }
        if (!socialsList.equals(originalSocials)) updates.put("socials", socialsList);

        if (!updates.isEmpty()) {
            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        showCustomToast("Profile updated!");
                        originalData.put("username", etUsername.getText().toString());
                        originalData.put("fullName", etFullName.getText().toString());
                        originalData.put("bio", etBio.getText().toString());
                        originalData.put("location", etLocation.getText().toString());
                        originalSocials = new ArrayList<>(socialsList);

                        finish();
                    })
                    .addOnFailureListener(e -> showCustomToast("Failed to update profile"));
        } else {
            showCustomToast("No changes to save");
        }
    }

    private void checkAndPutUpdate(String key, String newValue, Map<String, Object> updates) {
        String original = originalData.get(key);
        if (original == null || !original.equals(newValue)) updates.put(key, newValue);
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Photo"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivProfileImage.setImageURI(imageUri);
            uploadProfilePhoto();
        }
    }

    private void uploadProfilePhoto() {
        if (imageUri == null) return;

        MediaManager.get().upload(imageUri)
                .option("folder", "profile_photos")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String newUrl = (String) resultData.get("secure_url");

                        db.collection("users").document(userId)
                                .update("profilePhoto", newUrl)
                                .addOnSuccessListener(aVoid -> {
                                    Glide.with(EditProfileActivity.this).load(newUrl).circleCrop().into(ivProfileImage);
                                    showCustomToast("Profile photo updated!");

                                    oldProfileUrl = newUrl;
                                });
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        showCustomToast("Upload failed: " + error.getDescription());
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();

    }

    private String getPublicIdFromUrl(String url) {
        int index = url.lastIndexOf("/upload/");
        int dotIndex = url.lastIndexOf('.');
        return url.substring(index + 8, dotIndex);
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
