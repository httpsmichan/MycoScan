package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class UserProfile extends AppCompatActivity {

    private Button btnFollow;
    private Button btnAboutTab, btnPostsTab;
    private View layoutAboutContent, layoutPostsContent;
    private TextView tvMobile, tvEmail;
    private LinearLayout layoutSocialLinks;

    private TextView tvUserHandle, tvUsername, tvFollowersCount, tvFollowingCount, tvPostsCount, tvBio;
    private com.github.mikephil.charting.charts.LineChart dailyContributionChart;

    private TextView cardValue1, cardValue2, cardValue3;
    private ImageView ivProfilePhoto;
    private RecyclerView recyclerUserPosts;
    private FirebaseFirestore db;
    private String visitedUserId;
    private String visitedUsername;
    private String currentUserId;
    private boolean isFollowing = false;
    private UserPostsGridAdapter userPostsAdapter;
    private ImageView imageVerifiedBadge;
    private final List<Post> userPostsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        visitedUserId = getIntent().getStringExtra("visitedUserId");
        visitedUsername = getIntent().getStringExtra("visitedUsername");

        initViews();

        recyclerUserPosts = findViewById(R.id.recyclerUserPosts);
        recyclerUserPosts.setLayoutManager(new GridLayoutManager(this, 3));
        userPostsAdapter = new UserPostsGridAdapter(this, userPostsList);
        recyclerUserPosts.setAdapter(userPostsAdapter);
        dailyContributionChart = findViewById(R.id.dailyContributionChart);
        tvMobile = findViewById(R.id.tvMobile);
        tvEmail = findViewById(R.id.tvEmail);
        layoutSocialLinks = findViewById(R.id.layoutSocialLinks);

        loadUserProfile();

        checkFollowStatus();

        loadUserPosts(visitedUserId);
        loadUserPostCount(visitedUserId);
        loadVerifiedPostCount(visitedUserId);
        loadUserScanCount(visitedUserId);
        loadDailyContributions(visitedUserId);

        btnFollow.setOnClickListener(v -> toggleFollow());

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

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return 1;
            }
        });
        recyclerUserPosts.setLayoutManager(gridLayoutManager);
    }

    private void addItemDecoration() {
        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        recyclerUserPosts.addItemDecoration(new GridSpacingItemDecoration(3, spacing, true));
    }

    private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, android.view.View view,
                                   RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }

    private void initViews() {
        btnFollow = findViewById(R.id.btnFollow);
        tvUserHandle = findViewById(R.id.textUserHandle);
        tvUsername = findViewById(R.id.textUsername);
        tvFollowersCount = findViewById(R.id.textFollowers);
        tvFollowingCount = findViewById(R.id.textFollowing);
        tvPostsCount = findViewById(R.id.textPosts);
        tvBio = findViewById(R.id.textUserBio);
        ivProfilePhoto = findViewById(R.id.imageProfile);
        recyclerUserPosts = findViewById(R.id.recyclerUserPosts);
        btnAboutTab = findViewById(R.id.btnAboutTab);
        btnPostsTab = findViewById(R.id.btnPostsTab);
        layoutAboutContent = findViewById(R.id.layoutAboutContent);
        layoutPostsContent = findViewById(R.id.layoutPostsContent);
        imageVerifiedBadge = findViewById(R.id.imageVerifiedBadge);
        cardValue1 = findViewById(R.id.cardValue1);
        cardValue2 = findViewById(R.id.cardValue2);
        cardValue3 = findViewById(R.id.cardValue3);


        if (visitedUserId != null && visitedUserId.equals(currentUserId)) {
            btnFollow.setVisibility(View.GONE);
        }
    }

    private void loadUserProfile() {
        if (visitedUserId == null) {
            if (visitedUsername != null) {
                findUserByUsername(visitedUsername);
            } else {
                showCustomToast("Error: No user specified");
                finish();
            }
            return;
        }

        db.collection("users").document(visitedUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        populateUserProfile(document);
                    } else {
                        showCustomToast("User not found");
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfile", "Error loading user profile", e);
                    showCustomToast("Error loading profile");
                });

    }

    private void loadUserPostCount(String userId) {
        db.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null) {
                        int postCount = querySnapshot.size();
                        cardValue1.setText(String.valueOf(postCount));
                        Log.d("UserProfile", "Post count for user " + userId + ": " + postCount);
                    } else {
                        cardValue1.setText("0");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfile", "Error getting post count", e);
                    cardValue1.setText("0");
                });
    }

    private void findUserByUsername(String username) {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        visitedUserId = document.getId();
                        populateUserProfile(document);
                        checkFollowStatus();
                        loadUserPosts(visitedUserId);

                        if (!visitedUserId.equals(currentUserId)) {
                            btnFollow.setVisibility(View.VISIBLE);
                        }
                    } else {
                        showCustomToast("User not found");
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfile", "Error finding user by username", e);
                    showCustomToast("Error loading profile");
                });

    }

    private void loadVerifiedPostCount(String userId) {
        db.collection("posts")
                .whereEqualTo("userId", userId)
                .whereEqualTo("verified", "verified")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int verifiedCount = querySnapshot.size();
                    cardValue3.setText(String.valueOf(verifiedCount));
                })
                .addOnFailureListener(e -> {
                    Log.e("SettingsFragment", "Failed to load verified posts", e);
                    cardValue3.setText("0");
                });
    }

    private void loadUserScanCount(String userId) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long scannedValue = documentSnapshot.getLong("scanned");
                        if (scannedValue != null) {
                            cardValue2.setText(String.valueOf(scannedValue));
                        } else {
                            cardValue2.setText("0");
                        }
                    } else {
                        cardValue2.setText("0");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfile", "Error loading scan count", e);
                    cardValue2.setText("0");
                });
    }

    private void loadDailyContributions(String userId) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        db.collection("posts")
                .whereEqualTo("userId", userId)
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
                            .document(userId)
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
                                leftAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                                    @Override
                                    public String getFormattedValue(float value) {
                                        return String.valueOf((int) value);
                                    }
                                });

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
                                Log.e("UserProfile", "Failed to load scans for chart", e);
                            });
                })
                .addOnFailureListener(e -> Log.e("UserProfile", "Failed to load posts for chart", e));
    }

    private void populateUserProfile(DocumentSnapshot document) {

        String mobile = document.getString("mobile");
        String email = document.getString("email");

        if (mobile != null && !mobile.isEmpty()) {
            tvMobile.setText("Mobile: " + mobile);
            tvMobile.setVisibility(View.VISIBLE);
        } else {
            tvMobile.setVisibility(View.GONE);
        }

        if (email != null && !email.isEmpty()) {
            tvEmail.setText("Email: " + email);
            tvEmail.setVisibility(View.VISIBLE);
        } else {
            tvEmail.setVisibility(View.GONE);
        }


        List<String> socials = (List<String>) document.get("socials");
        Log.d("UserProfile", "Socials data: " + socials);

        layoutSocialLinks.removeAllViews();

        if (socials != null && !socials.isEmpty()) {
            for (String link : socials) {
                Log.d("UserProfile", "Adding link: " + link);

                TextView tvLink = new TextView(this);
                tvLink.setText(link);
                tvLink.setTextColor(Color.parseColor("#111827"));
                tvLink.setTextSize(12);
                tvLink.setPaintFlags(tvLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 15);
                tvLink.setLayoutParams(params);

                layoutSocialLinks.addView(tvLink);
                Log.d("UserProfile", "Link added successfully");
            }
        } else {
            Log.d("UserProfile", "No socials found");
            layoutSocialLinks.setVisibility(View.GONE);
        }

        Log.d("UserProfile", "Final child count: " + layoutSocialLinks.getChildCount());
        Log.d("UserProfile", "layoutSocialLinks visibility after: " + layoutSocialLinks.getVisibility());

        String username = document.getString("username");
        if (username != null) {
            tvUserHandle.setText(username);
            visitedUsername = username;
        }

        String fullName = document.getString("fullName");
        if (fullName != null) {
            tvUsername.setText(fullName);
        }

        String bio = document.getString("bio");
        if (bio != null && !bio.isEmpty()) {
            tvBio.setText(bio);
            tvBio.setVisibility(View.VISIBLE);
        } else {
            tvBio.setVisibility(View.GONE);
        }

        String profilePhoto = document.getString("profilePhoto");
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            Glide.with(this)
                    .load(profilePhoto)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .circleCrop()
                    .into(ivProfilePhoto);
        }

        Long followers = document.getLong("followers");
        Long following = document.getLong("following");
        tvFollowersCount.setText(String.valueOf(followers != null ? followers : 0));
        tvFollowingCount.setText(String.valueOf(following != null ? following : 0));

        db.collection("applications")
                .whereEqualTo("userId", visitedUserId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String status = query.getDocuments().get(0).getString("status");
                        imageVerifiedBadge.setVisibility("approved".equalsIgnoreCase(status)
                                ? View.VISIBLE : View.GONE);
                    } else {
                        imageVerifiedBadge.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfile", "Error checking verification status", e);
                    imageVerifiedBadge.setVisibility(View.GONE);
                });
    }

    private void checkFollowStatus() {
        if (visitedUserId == null || currentUserId == null || visitedUserId.equals(currentUserId)) {
            return;
        }

        db.collection("users")
                .document(visitedUserId)
                .collection("followersList")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    isFollowing = document.exists();
                    updateFollowButtonText();
                })
                .addOnFailureListener(e -> Log.e("UserProfile", "Error checking follow status", e));
    }

    private void loadUserPosts(String userId) {
        db.collection("posts")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("UserProfile", "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null) {
                        userPostsList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Post post = doc.toObject(Post.class);
                            post.setPostId(doc.getId());
                            userPostsList.add(post);
                            Log.d("UserProfile", "Loaded user post ID: " + doc.getId());
                        }
                        userPostsList.sort((p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));

                        userPostsAdapter.notifyDataSetChanged();
                        tvPostsCount.setText(String.valueOf(userPostsList.size()));
                        Log.d("UserProfile", "Loaded " + userPostsList.size() + " posts");
                    }
                });
    }


    private void toggleFollow() {
        if (visitedUserId == null || currentUserId == null) {
            showCustomToast("Error: Missing user info");
            return;
        }

        if (visitedUserId.equals(currentUserId)) {
            showCustomToast("Cannot follow yourself");
            return;
        }

        btnFollow.setEnabled(false);

        DocumentReference visitedUserRef = db.collection("users").document(visitedUserId);
        DocumentReference currentUserRef = db.collection("users").document(currentUserId);

        if (!isFollowing) {

            Map<String, Object> followerData = new HashMap<>();
            followerData.put("userId", currentUserId);
            followerData.put("timestamp", System.currentTimeMillis());

            Map<String, Object> followingData = new HashMap<>();
            followingData.put("userId", visitedUserId);
            followingData.put("timestamp", System.currentTimeMillis());

            visitedUserRef.collection("followersList").document(currentUserId).set(followerData);
            currentUserRef.collection("followingList").document(visitedUserId).set(followingData);

            visitedUserRef.update("followers", FieldValue.increment(1))
                    .addOnSuccessListener(aVoid -> {
                        currentUserRef.update("following", FieldValue.increment(1))
                                .addOnSuccessListener(aVoid1 -> {
                                    isFollowing = true;
                                    updateFollowButtonText();
                                    updateFollowersCount(1);
                                    btnFollow.setEnabled(true);
                                    showCustomToast("Now following " + visitedUsername);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("UserProfile", "Error updating following count", e);
                                    btnFollow.setEnabled(true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("UserProfile", "Error updating followers count", e);
                        btnFollow.setEnabled(true);
                    });

        } else {
            visitedUserRef.collection("followersList").document(currentUserId).delete();
            currentUserRef.collection("followingList").document(visitedUserId).delete();

            visitedUserRef.update("followers", FieldValue.increment(-1))
                    .addOnSuccessListener(aVoid -> {
                        currentUserRef.update("following", FieldValue.increment(-1))
                                .addOnSuccessListener(aVoid1 -> {
                                    isFollowing = false;
                                    updateFollowButtonText();
                                    updateFollowersCount(-1);
                                    btnFollow.setEnabled(true);
                                    showCustomToast("Unfollowed " + visitedUsername);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("UserProfile", "Error updating following count", e);
                                    btnFollow.setEnabled(true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("UserProfile", "Error updating followers count", e);
                        btnFollow.setEnabled(true);
                    });
        }
    }

    private void updateFollowButtonText() {
        if (btnFollow != null) {
            btnFollow.setText(isFollowing ? "Unfollow" : "Follow");
        }
    }

    private void updateFollowersCount(int change) {
        int currentCount = Integer.parseInt(tvFollowersCount.getText().toString());
        tvFollowersCount.setText(String.valueOf(currentCount + change));
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