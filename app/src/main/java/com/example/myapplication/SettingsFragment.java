package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class SettingsFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1001;

    private TextView textUsername, textUserHandle, textUserBio, textFollowers, textFollowing, textPosts;
    private TextView btnEditProfile, btnJournal, btnLogout, btnVerify;
    private Button btnAboutTab, btnPostsTab;
    private LinearLayout layoutAboutContent, layoutPostsContent;

    private RecyclerView recyclerUserPosts;
    private UserPostAdapter postAdapter;
    private List<Post> postList;
    private ImageView imageProfile, imageVerifiedBadge;
    private TextView cardValue1, cardValue2, cardValue3;

    private FirebaseFirestore db;
    private Uri selectedImageUri;
    private TextView btnVerifyPosts;
    private FirebaseUser currentUser;
    private com.github.mikephil.charting.charts.LineChart dailyContributionChart;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_settings_fragment, container, false);

        imageProfile = view.findViewById(R.id.imageProfile);
        textUsername = view.findViewById(R.id.textUsername);
        textUserHandle = view.findViewById(R.id.textUserHandle);
        textUserBio = view.findViewById(R.id.textUserBio);
        textFollowers = view.findViewById(R.id.textFollowers);
        textFollowing = view.findViewById(R.id.textFollowing);
        textPosts = view.findViewById(R.id.textPosts);
        imageVerifiedBadge = view.findViewById(R.id.imageVerifiedBadge);

        btnAboutTab = view.findViewById(R.id.btnAboutTab);
        btnPostsTab = view.findViewById(R.id.btnPostsTab);
        layoutAboutContent = view.findViewById(R.id.layoutAboutContent);
        layoutPostsContent = view.findViewById(R.id.layoutPostsContent);

        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnJournal = view.findViewById(R.id.btnJournal);
        btnVerify = view.findViewById(R.id.btnVerify);
        btnLogout = view.findViewById(R.id.btnLogout);

        btnVerifyPosts = view.findViewById(R.id.btnVerifyPosts);
        btnVerifyPosts.setVisibility(View.GONE);

        recyclerUserPosts = view.findViewById(R.id.recyclerUserPosts);
        recyclerUserPosts.setLayoutManager(new GridLayoutManager(getContext(), 3));

        postList = new ArrayList<>();
        postAdapter = new UserPostAdapter(getContext(), postList);
        recyclerUserPosts.setAdapter(postAdapter);
        dailyContributionChart = view.findViewById(R.id.dailyContributionChart);

        cardValue1 = view.findViewById(R.id.cardValue1);
        cardValue2 = view.findViewById(R.id.cardValue2);
        cardValue3 = view.findViewById(R.id.cardValue3);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {

            db.collection("posts")
                    .whereEqualTo("userId", user.getUid())
                    .get()
                    .addOnSuccessListener(querySnapshot -> cardValue1.setText(String.valueOf(querySnapshot.size())));


            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Long scanned = documentSnapshot.getLong("scanned");
                        cardValue2.setText(String.valueOf(scanned != null ? scanned : 0));
                    });


            db.collection("posts")
                    .whereEqualTo("userId", user.getUid())
                    .whereEqualTo("verified", "verified")
                    .get()
                    .addOnSuccessListener(querySnapshot ->
                            cardValue3.setText(String.valueOf(querySnapshot.size()))
                    )
                    .addOnFailureListener(e -> {
                        Log.e("SettingsFragment", "Error loading verified posts", e);
                        cardValue3.setText("0");
                    });


            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isVerified = documentSnapshot.getBoolean("verified");
                            if (isVerified != null && isVerified) {
                                btnVerifyPosts.setVisibility(View.VISIBLE);


                                btnVerifyPosts.setOnClickListener(v -> {
                                    Intent intent = new Intent(getActivity(), VerifiedAccount.class);
                                    startActivity(intent);
                                });

                            } else {
                                btnVerifyPosts.setVisibility(View.GONE);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("SettingsFragment", "Failed to check verified status", e);
                        btnVerifyPosts.setVisibility(View.GONE);
                    });
        }

        setupDailyContributionChart();
        loadUserInfoAndPosts();

        imageProfile.setOnClickListener(v -> openImagePicker());
        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));
        btnJournal.setOnClickListener(v -> startActivity(new Intent(getActivity(), JournalActivity.class)));

        btnVerify.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Verification.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        btnAboutTab.setOnClickListener(v -> {
            layoutAboutContent.setVisibility(View.VISIBLE);
            layoutPostsContent.setVisibility(View.GONE);
            btnAboutTab.setBackgroundResource(R.drawable.tab_selected_background);
            btnPostsTab.setBackgroundResource(R.drawable.tab_unselected_background);
        });

        btnPostsTab.setOnClickListener(v -> {
            layoutAboutContent.setVisibility(View.GONE);
            layoutPostsContent.setVisibility(View.VISIBLE);
            btnPostsTab.setBackgroundResource(R.drawable.tab_selected_background);
            btnAboutTab.setBackgroundResource(R.drawable.tab_unselected_background);
        });

        return view;
    }

    private void loadUserInfoAndPosts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("fullName");
                            String username = documentSnapshot.getString("username");
                            Long followersCount = documentSnapshot.getLong("followers");
                            Long followingCount = documentSnapshot.getLong("following");
                            String bio = documentSnapshot.getString("bio");

                            if (fullName != null && !fullName.isEmpty()) {
                                textUsername.setText(fullName);
                            } else {
                                textUsername.setText("User");
                            }

                            if (username != null && !username.isEmpty()) {
                                textUserHandle.setText("@" + username);
                            } else {
                                textUserHandle.setText("@unknown");
                            }

                            if (followersCount != null) {
                                textFollowers.setText(String.valueOf(followersCount));
                            } else {
                                textFollowers.setText("0");
                            }

                            if (followingCount != null) {
                                textFollowing.setText(String.valueOf(followingCount));
                            } else {
                                textFollowing.setText("0");
                            }

                            if (bio != null && !bio.isEmpty()) {
                                textUserBio.setText(bio);
                            } else {
                                textUserBio.setText("No bio yet.");
                            }

                            db.collection("applications")
                                    .whereEqualTo("userId", user.getUid())
                                    .get()
                                    .addOnSuccessListener(query -> {
                                        if (!query.isEmpty()) {
                                            String status = query.getDocuments().get(0).getString("status");
                                            if ("approved".equalsIgnoreCase(status)) {
                                                imageVerifiedBadge.setVisibility(View.VISIBLE);
                                            } else {
                                                imageVerifiedBadge.setVisibility(View.GONE);
                                            }
                                        }
                                    });

                            String photoUrl = documentSnapshot.getString("profilePhoto");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                if (isAdded() && getContext() != null) {
                                    Glide.with(requireContext())
                                            .load(photoUrl)
                                            .placeholder(R.drawable.ic_person_placeholder)
                                            .error(R.drawable.ic_person_placeholder)
                                            .circleCrop()
                                            .into(imageProfile);
                                }
                            } else {
                                imageProfile.setImageResource(R.drawable.ic_person_placeholder);
                            }

                        }
                    });

            loadUserPosts(user.getUid());
        } else {
            textUsername.setText("No user signed in");
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST);
    }

    private void setupDailyContributionChart() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("posts")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(postsSnapshot -> {
                    Map<String, Integer> postsPerDay = new TreeMap<>();

                    for (QueryDocumentSnapshot doc : postsSnapshot) {
                        Long timestamp = doc.getLong("timestamp");
                        if (timestamp == null) continue;

                        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                .format(new java.util.Date(timestamp));
                        postsPerDay.put(date, postsPerDay.getOrDefault(date, 0) + 1);
                    }

                    db.collection("users")
                            .document(user.getUid())
                            .collection("scanned")
                            .get()
                            .addOnSuccessListener(scansSnapshot -> {
                                Map<String, Integer> scansPerDay = new TreeMap<>();

                                for (QueryDocumentSnapshot doc : scansSnapshot) {
                                    com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
                                    if (timestamp == null) continue;

                                    String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                            .format(timestamp.toDate());
                                    scansPerDay.put(date, scansPerDay.getOrDefault(date, 0) + 1);
                                }

                                Set<String> allDates = new TreeSet<>();
                                allDates.addAll(postsPerDay.keySet());
                                allDates.addAll(scansPerDay.keySet());

                                List<com.github.mikephil.charting.data.Entry> postsEntries = new ArrayList<>();
                                List<com.github.mikephil.charting.data.Entry> scansEntries = new ArrayList<>();
                                List<String> xLabels = new ArrayList<>();
                                int index = 0;

                                postsEntries.add(new com.github.mikephil.charting.data.Entry(index, 0));
                                scansEntries.add(new com.github.mikephil.charting.data.Entry(index, 0));
                                xLabels.add("");
                                index++;

                                for (String date : allDates) {
                                    int postCount = postsPerDay.getOrDefault(date, 0);
                                    int scanCount = scansPerDay.getOrDefault(date, 0);

                                    postsEntries.add(new com.github.mikephil.charting.data.Entry(index, postCount));
                                    scansEntries.add(new com.github.mikephil.charting.data.Entry(index, scanCount));
                                    xLabels.add(date);
                                    index++;
                                }

                                com.github.mikephil.charting.data.LineDataSet postsDataSet =
                                        new com.github.mikephil.charting.data.LineDataSet(postsEntries, "Daily Posts");
                                postsDataSet.setColor(android.graphics.Color.parseColor("#4287f5"));
                                postsDataSet.setCircleColor(android.graphics.Color.parseColor("#4287f5"));
                                postsDataSet.setCircleRadius(3f);
                                postsDataSet.setLineWidth(2f);
                                postsDataSet.setDrawFilled(true);
                                postsDataSet.setFillColor(android.graphics.Color.parseColor("#FFE0DE"));
                                postsDataSet.setValueTextSize(8f);
                                postsDataSet.setDrawValues(false);
                                postsDataSet.setValueTextColor(android.graphics.Color.DKGRAY);
                                postsDataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER);

                                com.github.mikephil.charting.data.LineDataSet scansDataSet =
                                        new com.github.mikephil.charting.data.LineDataSet(scansEntries, "Mushroom Scans");
                                scansDataSet.setColor(android.graphics.Color.parseColor("#54d166"));
                                scansDataSet.setCircleColor(android.graphics.Color.parseColor("#54d166"));
                                scansDataSet.setCircleRadius(3f);
                                scansDataSet.setLineWidth(2f);
                                scansDataSet.setDrawFilled(true);
                                scansDataSet.setFillColor(android.graphics.Color.parseColor("#C8E6C9"));
                                scansDataSet.setValueTextSize(8f);
                                scansDataSet.setDrawValues(false);
                                scansDataSet.setValueTextColor(android.graphics.Color.DKGRAY);
                                scansDataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER);

                                com.github.mikephil.charting.data.LineData lineData =
                                        new com.github.mikephil.charting.data.LineData(postsDataSet, scansDataSet);
                                dailyContributionChart.setData(lineData);

                                com.github.mikephil.charting.components.XAxis xAxis = dailyContributionChart.getXAxis();
                                xAxis.setGranularity(1f);
                                xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
                                xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                                    @Override
                                    public String getFormattedValue(float value) {
                                        int i = Math.round(value);
                                        if (i >= 0 && i < xLabels.size()) return xLabels.get(i);
                                        return "";
                                    }
                                });
                                xAxis.setTextColor(android.graphics.Color.DKGRAY);
                                xAxis.setDrawGridLines(false);

                                com.github.mikephil.charting.components.YAxis leftAxis = dailyContributionChart.getAxisLeft();
                                leftAxis.setGranularity(1f);
                                leftAxis.setGranularityEnabled(true);
                                leftAxis.setTextColor(android.graphics.Color.DKGRAY);
                                leftAxis.setAxisMinimum(0f);
                                leftAxis.setDrawGridLines(true);

                                dailyContributionChart.getAxisRight().setEnabled(false);

                                com.github.mikephil.charting.components.Legend legend = dailyContributionChart.getLegend();
                                legend.setTextColor(android.graphics.Color.DKGRAY);
                                legend.setEnabled(true);
                                legend.setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
                                legend.setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
                                legend.setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
                                legend.setDrawInside(false);

                                dailyContributionChart.getDescription().setEnabled(false);
                                dailyContributionChart.animateX(1000);
                                dailyContributionChart.invalidate();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("SettingsFragment", "Failed to load scans for chart", e);

                                displayPostsOnly(postsPerDay);
                            });
                })
                .addOnFailureListener(e -> Log.e("SettingsFragment", "Failed to load posts for chart", e));
    }

    private void displayPostsOnly(Map<String, Integer> postsPerDay) {
        List<com.github.mikephil.charting.data.Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        int index = 0;

        entries.add(new com.github.mikephil.charting.data.Entry(index, 0));
        xLabels.add("");
        index++;

        for (Map.Entry<String, Integer> entry : postsPerDay.entrySet()) {
            entries.add(new com.github.mikephil.charting.data.Entry(index, entry.getValue()));
            xLabels.add(entry.getKey());
            index++;
        }

        com.github.mikephil.charting.data.LineDataSet dataSet =
                new com.github.mikephil.charting.data.LineDataSet(entries, "Daily Posts");
        dataSet.setColor(android.graphics.Color.parseColor("#c74138"));
        dataSet.setCircleColor(android.graphics.Color.parseColor("#54d166"));
        dataSet.setCircleRadius(3f);
        dataSet.setLineWidth(1f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(android.graphics.Color.parseColor("#C8E6C9"));
        dataSet.setValueTextSize(8f);
        dataSet.setDrawValues(false);
        dataSet.setValueTextColor(android.graphics.Color.DKGRAY);
        dataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER);

        com.github.mikephil.charting.data.LineData lineData = new com.github.mikephil.charting.data.LineData(dataSet);
        dailyContributionChart.setData(lineData);

        com.github.mikephil.charting.components.XAxis xAxis = dailyContributionChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = Math.round(value);
                if (i >= 0 && i < xLabels.size()) return xLabels.get(i);
                return "";
            }
        });
        xAxis.setTextColor(android.graphics.Color.DKGRAY);
        xAxis.setDrawGridLines(false);

        com.github.mikephil.charting.components.YAxis leftAxis = dailyContributionChart.getAxisLeft();
        leftAxis.setGranularity(1f);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setTextColor(android.graphics.Color.DKGRAY);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        dailyContributionChart.getAxisRight().setEnabled(false);
        dailyContributionChart.getLegend().setTextColor(android.graphics.Color.DKGRAY);
        dailyContributionChart.getDescription().setEnabled(false);
        dailyContributionChart.animateX(1000);
        dailyContributionChart.invalidate();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            if (isAdded() && getContext() != null) {
                Glide.with(requireContext())
                        .load(selectedImageUri)
                        .placeholder(R.drawable.ic_person_placeholder)
                        .error(R.drawable.ic_person_placeholder)
                        .circleCrop()
                        .into(imageProfile);
            }

            uploadImageToCloudinary(selectedImageUri);
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        if (imageUri == null) return;

        MediaManager.get().upload(imageUri)
                .option("folder", "profile_photos")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) { }
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) { }
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String cloudinaryUrl = (String) resultData.get("secure_url");
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            db.collection("users").document(user.getUid())
                                    .update("profilePhoto", cloudinaryUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        showCustomToast("Profile photo updated!");
                                        db.collection("logs").add(new LogEntry(
                                                user.getUid(),
                                                "uploaded a profile photo",
                                                System.currentTimeMillis()
                                        ));
                                        Log.d("SettingsFragment", "User " + user.getUid() + " uploaded a profile photo");
                                    })
                                    .addOnFailureListener(e -> Log.e("SettingsFragment", "Failed to update profile photo", e));
                        }
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        Log.e("SettingsFragment", "Cloudinary upload error: " + error.getDescription());
                        showCustomToast("Failed to upload image");
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w("SettingsFragment", "Cloudinary upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();
    }

    private void loadUserPosts(String userId) {
        db.collection("posts")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("SettingsFragment", "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null) {
                        postList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Post post = doc.toObject(Post.class);
                            post.setPostId(doc.getId());
                            postList.add(post);
                            Log.d("SettingsFragment", "Loaded user post ID: " + doc.getId());
                        }

                        Collections.sort(postList, (p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));
                        postAdapter.notifyDataSetChanged();

                        textPosts.setText(String.valueOf(postList.size()));


                        TextView noPosts = getView().findViewById(R.id.noPosts);
                        RecyclerView recyclerUserPosts = getView().findViewById(R.id.recyclerUserPosts);

                        if (postList.isEmpty()) {

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.gravity = android.view.Gravity.CENTER;
                            noPosts.setLayoutParams(params);

                            noPosts.setVisibility(View.VISIBLE);
                            recyclerUserPosts.setVisibility(View.GONE);
                        } else {
                            noPosts.setVisibility(View.GONE);
                            recyclerUserPosts.setVisibility(View.VISIBLE);
                        }

                        Log.d("SettingsFragment", "Loaded " + postList.size() + " posts");
                    }
                });
    }

    public static class LogEntry {
        private String userId;
        private String action;
        private long timestamp;

        public LogEntry() { }
        public LogEntry(String userId, String action, long timestamp) {
            this.userId = userId;
            this.action = action;
            this.timestamp = timestamp;
        }

        public String getUserId() { return userId; }
        public String getAction() { return action; }
        public long getTimestamp() { return timestamp; }
    }

    private void showCustomToast(String message) {
        if (!isAdded()) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View layout = inflater.inflate(R.layout.custom_toast, null);

        TextView toastText = layout.findViewById(R.id.toast_text);
        toastText.setText(message);

        Toast toast = new Toast(requireContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

}
