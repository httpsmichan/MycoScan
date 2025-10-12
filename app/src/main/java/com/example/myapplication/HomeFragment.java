package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.app.AlertDialog;

public class HomeFragment extends Fragment {

    private FloatingActionButton fabReport;
    private RecyclerView recyclerPosts, searchSuggestionsRecycler;
    private PostAdapter adapter;
    private final List<Post> postList = new ArrayList<>();
    private FirebaseFirestore db;

    private ImageView tipImageView, profileIcon;
    private ImageView properHarvestingImage, identificationTipsImage, mushroomEncy;
    private TextView tipTextView;

    private CardView cardProperHarvesting, cardIdentificationTips;

    private EditText searchEditText;
    private SearchSuggestionAdapter suggestionAdapter;
    private final List<String> suggestionList = new ArrayList<>();
    private final List<String> mushroomTypesList = new ArrayList<>();
    private final List<UserInfo> usersList = new ArrayList<>();
    private LinearLayout tipCardsContainer;


    private static class UserInfo {
        String userId;
        String username;

        UserInfo(String userId, String username) {
            this.userId = userId;
            this.username = username;
        }
    }

    private static class TipCategory {
        String name;
        String imageUrl;
        List<String> tips;

        TipCategory(String name, String imageUrl, List<String> tips) {
            this.name = name;
            this.imageUrl = imageUrl;
            this.tips = tips;
        }
    }

    public HomeFragment() {}

    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        if (!isAdded()) {
            return view;
        }

        recyclerPosts = view.findViewById(R.id.recyclerPosts);
        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerPosts.setHasFixedSize(true);
        adapter = new PostAdapter(requireContext(), postList);
        recyclerPosts.setAdapter(adapter);

        tipTextView = view.findViewById(R.id.tipTextView);
        tipImageView = view.findViewById(R.id.tip_ImageView);
        profileIcon = view.findViewById(R.id.profileIcon);
        properHarvestingImage = view.findViewById(R.id.proper_harvesting_image);
        identificationTipsImage = view.findViewById(R.id.identification_tips_image);

        cardProperHarvesting = view.findViewById(R.id.card_proper_harvesting);
        cardIdentificationTips = view.findViewById(R.id.card_identification_tips);

        searchEditText = view.findViewById(R.id.searchEditText);
        searchSuggestionsRecycler = view.findViewById(R.id.searchSuggestionsRecycler);

        suggestionAdapter = new SearchSuggestionAdapter(suggestionList);
        searchSuggestionsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchSuggestionsRecycler.setAdapter(suggestionAdapter);
        tipCardsContainer = view.findViewById(R.id.tipCardsContainer);
        mushroomEncy = view.findViewById(R.id.mushroomEncy);


        fabReport = view.findViewById(R.id.fabReport);
        fabReport.setOnClickListener(v -> showReportDialog());

        fabReport.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private long touchStart;
            private static final int CLICK_THRESHOLD = 200;
            private static final int MOVE_THRESHOLD = 10;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        touchStart = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        view.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        return true;

                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - touchStart;

                        if (duration < CLICK_THRESHOLD &&
                                Math.abs(event.getRawX() + dX - view.getX()) < MOVE_THRESHOLD &&
                                Math.abs(event.getRawY() + dY - view.getY()) < MOVE_THRESHOLD) {
                            view.performClick();
                        }
                        return true;
                }
                return false;
            }
        });

        final String[] categories = {"Edible", "Inedible", "Poisonous"};

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim();

                tipCardsContainer.setVisibility(input.isEmpty() ? View.VISIBLE : View.GONE);
                suggestionList.clear();
                boolean matchedCategory = false, matchedMushroomType = false, matchedUser = false;

                if (!input.isEmpty()) {

                    for (String cat : categories) {
                        if (cat.toLowerCase().contains(input.toLowerCase())) {
                            suggestionList.add("Category: " + cat);
                        }
                        if (cat.equalsIgnoreCase(input)) matchedCategory = true;
                    }

                    for (String mushroomType : mushroomTypesList) {
                        if (mushroomType.toLowerCase().contains(input.toLowerCase())) {
                            suggestionList.add("Mushroom: " + mushroomType);
                        }
                        if (mushroomType.equalsIgnoreCase(input)) matchedMushroomType = true;
                    }

                    for (UserInfo user : usersList) {
                        if (user.username.toLowerCase().contains(input.toLowerCase())) {
                            suggestionList.add("User: " + user.username);
                        }
                        if (user.username.equalsIgnoreCase(input)) matchedUser = true;
                    }
                }

                suggestionAdapter.notifyDataSetChanged();
                searchSuggestionsRecycler.setVisibility(suggestionList.isEmpty() ? View.GONE : View.VISIBLE);

                if (input.isEmpty()) {
                    loadAllPosts();
                } else if (matchedCategory) {
                    filterPostsByCategory(input);
                } else if (matchedMushroomType) {
                    filterPostsByMushroomType(input);
                } else if (matchedUser) {
                    filterPostsByUser(input);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        mushroomEncy.setOnClickListener(v -> {
            DrawerLayout drawerLayout = requireActivity().findViewById(R.id.drawerLayout);
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });


        suggestionAdapter.setOnItemClickListener(item -> {
            if (item.startsWith("User: ")) {
                String username = item.replace("User: ", "").trim();
                redirectToUserProfile(username);
            } else {
                searchEditText.setText(item);
                searchSuggestionsRecycler.setVisibility(View.GONE);

                if (item.startsWith("Category: ")) {
                    filterPostsByCategory(item.replace("Category: ", "").trim());
                } else if (item.startsWith("Mushroom: ")) {
                    filterPostsByMushroomType(item.replace("Mushroom: ", "").trim());
                } else {
                    loadAllPosts();
                }
            }
        });

        cardProperHarvesting.setOnClickListener(v -> {
            if (isAdded()) startActivity(new Intent(requireContext(), HarvestingActivity.class));
        });
        cardIdentificationTips.setOnClickListener(v -> {
            if (isAdded()) startActivity(new Intent(requireContext(), IdentificationActivity.class));
        });

        if (isAdded()) {
            Glide.with(this)
                    .load("https://res.cloudinary.com/diaw4uoea/image/upload/v1758248587/Gemini_Generated_Image_hk15g9hk15g9hk15_p1gppq.png")
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(properHarvestingImage);

            Glide.with(this)
                    .load("https://res.cloudinary.com/diaw4uoea/image/upload/v1758248582/Gemini_Generated_Image_uyfrw8uyfrw8uyfr_qbqyeo.png")
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(identificationTipsImage);
        }

        db = FirebaseFirestore.getInstance();
        loadAllPosts();
        loadMushroomTypes();
        loadUsers();
        loadProfilePhoto();

        List<TipCategory> categoriesList = loadCategoriesFromJson();
        if (!categoriesList.isEmpty()) {
            TipCategory chosen = getTimedCategoryTip(categoriesList);
            tipTextView.setText(chosen.tips.get(0));
            if (isAdded() && !chosen.imageUrl.isEmpty()) {
                Glide.with(this).load(chosen.imageUrl).into(tipImageView);
            }
        }

        return view;
    }

    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(snapshots -> {
                    usersList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String username = doc.getString("username");
                        if (username != null && !username.isEmpty()) {
                            usersList.add(new UserInfo(doc.getId(), username));
                        }
                    }
                    Log.d("HomeFragment", "Loaded " + usersList.size() + " users");
                })
                .addOnFailureListener(e -> Log.e("HomeFragment", "Error loading users", e));
    }

    private void redirectToUserProfile(String username) {

        UserInfo targetUser = null;
        for (UserInfo user : usersList) {
            if (user.username.equals(username)) {
                targetUser = user;
                break;
            }
        }

        if (targetUser != null) {

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && targetUser.userId.equals(currentUser.getUid())) {
                showCustomToast("This is your profile! Check the Profile tab.");
                return;
            }

            Intent intent = new Intent(requireContext(), UserProfile.class);
            intent.putExtra("visitedUserId", targetUser.userId);
            intent.putExtra("visitedUsername", targetUser.username);
            startActivity(intent);
        } else {
            showCustomToast("User not found");

        }

        searchEditText.setText("");
        searchSuggestionsRecycler.setVisibility(View.GONE);
    }

    private void filterPostsByUser(String username) {
        db.collection("posts")
                .whereEqualTo("username", username)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    postList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setPostId(doc.getId());
                        postList.add(post);
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("HomeFragment", "Filtered " + postList.size() + " posts by user: " + username);
                })
                .addOnFailureListener(e -> Log.e("HomeFragment", "Error filtering posts by user", e));
    }

    private void loadAllPosts() {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    postList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setPostId(doc.getId());

                        Log.d("HomeFragment", "Post ID: " + doc.getId());
                        Log.d("HomeFragment", "Image URL: " + post.getImageUrl());
                        Log.d("HomeFragment", "Username: " + post.getUsername());
                        Log.d("HomeFragment", "Mushroom Type: " + post.getMushroomType());

                        postList.add(post);
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("HomeFragment", "Loaded " + postList.size() + " posts");
                })
                .addOnFailureListener(e -> Log.e("HomeFragment", "Error loading posts", e));
    }

    private List<TipCategory> loadCategoriesFromJson() {

        List<TipCategory> categories = new ArrayList<>();

        if (!isAdded()) {
            return categories;
        }

        try {
            InputStream is = requireContext().getAssets().open("tips.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray("categories");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject catObject = jsonArray.getJSONObject(i);
                String name = catObject.getString("name");
                String imageUrl = catObject.getString("image_url");

                JSONArray tipsArray = catObject.getJSONArray("tips");
                List<String> tips = new ArrayList<>();
                for (int j = 0; j < tipsArray.length(); j++) {
                    tips.add(tipsArray.getString(j));
                }

                categories.add(new TipCategory(name, imageUrl, tips));
            }
        } catch (IOException | org.json.JSONException e) {
            Log.e("HomeFragment", "Error loading tips", e);
        }
        return categories;
    }

    private void loadProfilePhoto() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (!isAdded()) return;

                    if (document.exists()) {
                        String photoUrl = document.getString("profilePhoto");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.ic_settings)
                                    .circleCrop()
                                    .into(profileIcon);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("HomeFragment", "Failed to load profile photo", e));
    }

    private TipCategory getTimedCategoryTip(List<TipCategory> categories) {
        SharedPreferences prefs = requireContext().getSharedPreferences("TimedTipPrefs", requireContext().MODE_PRIVATE);

        long now = System.currentTimeMillis();
        long lastShown = prefs.getLong("last_timestamp", 0);

        long sixHours = 6 * 60 * 60 * 1000;

        if (now - lastShown >= sixHours) {
            Random random = new Random();
            TipCategory category = categories.get(random.nextInt(categories.size()));
            String randomTip = category.tips.get(random.nextInt(category.tips.size()));

            prefs.edit()
                    .putLong("last_timestamp", now)
                    .putString("tip_text", randomTip)
                    .putString("tip_image_url", category.imageUrl)
                    .apply();

            TipCategory chosen = new TipCategory(category.name, category.imageUrl, new ArrayList<>());
            chosen.tips.add(randomTip);
            return chosen;
        } else {
            String savedTip = prefs.getString("tip_text", "Stay curious about mushrooms!");
            String savedImage = prefs.getString("tip_image_url", "");

            TipCategory savedCategory = new TipCategory("Saved", savedImage, new ArrayList<>());
            savedCategory.tips.add(savedTip);
            return savedCategory;
        }
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


    private void showReportDialog() {
        if (!isAdded()) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_report, null);

        final EditText etProblem = dialogView.findViewById(R.id.etProblem);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String reportText = etProblem.getText().toString().trim();
            if (reportText.isEmpty()) {
                showCustomToast("Please enter something");
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String userId = (user != null) ? user.getUid() : "anonymous";

            HashMap<String, Object> report = new HashMap<>();
            report.put("label", "app report");
            report.put("message", reportText);
            report.put("userId", userId);
            report.put("timestamp", System.currentTimeMillis());

            FirebaseFirestore.getInstance().collection("reports")
                    .add(report)
                    .addOnSuccessListener(docRef -> {
                        showCustomToast("Report submitted. Thank you!");
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            showCustomToast("Failed to submit report: " + e.getMessage())
            );
        });

        dialog.show();
    }

    private void filterPostsByCategory(String category) {
        db.collection("posts")
                .whereEqualTo("category", category)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    postList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setPostId(doc.getId());
                        postList.add(post);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Log.e("HomeFragment", "Error filtering posts", e)
                );
    }

    private void loadMushroomTypes() {
        db.collection("posts")
                .get()
                .addOnSuccessListener(snapshots -> {

                    mushroomTypesList.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String mushroomType = doc.getString("mushroomType");
                        if (mushroomType != null && !mushroomType.isEmpty() && !mushroomTypesList.contains(mushroomType)) {
                            mushroomTypesList.add(mushroomType);
                        }
                    }
                    Log.d("HomeFragment", "Loaded " + mushroomTypesList.size() + " mushroom types");
                })
                .addOnFailureListener(e -> Log.e("HomeFragment", "Error loading mushroom types", e));
    }

    private void filterPostsByMushroomType(String mushroomType) {
        db.collection("posts")
                .whereEqualTo("mushroomType", mushroomType)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    postList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setPostId(doc.getId());
                        postList.add(post);
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("HomeFragment", "Filtered " + postList.size() + " posts by mushroom type: " + mushroomType);
                })
                .addOnFailureListener(e ->
                        Log.e("HomeFragment", "Error filtering posts by mushroom type", e)
                );
    }
}