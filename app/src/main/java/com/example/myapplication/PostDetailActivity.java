package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import android.graphics.drawable.GradientDrawable;

public class PostDetailActivity extends BaseActivity {


    private MapView miniMapView;
    private double latitude, longitude;

    private RecyclerView rvComments;
    private EditText etComment;
    private TextView btnPostComment;
    private List<Comment> commentList;
    private CommentAdapter commentAdapter;
    private FirebaseFirestore db;

    private String username;
    private String postId;
    private String postAuthorId;
    private ImageView btnUpvote, btnDownvote;
    private TextView tvUpvotes, tvDownvotes;
    private FirebaseUser currentUser;
    private ImageView ivMenuOptions, ivDetailImage;;

    private String currentUserId;
    private String currentUserVote = null;
    private TextView commentsCount;
    private String imageUrl;
    private View rootView;



    private List<String> bannedWords = new ArrayList<>();

    private void loadBannedWords() {
        try {
            InputStream is = getAssets().open("censored-words.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(json);

            String[] categories = {"profanity", "insults", "sexual", "violence", "drugs", "slurs"};

            for (String cat : categories) {
                JSONArray arr = obj.getJSONArray(cat);
                for (int i = 0; i < arr.length(); i++) {
                    String word = arr.getString(i).toLowerCase();
                    String regex = word.replaceAll(".", "$0+");
                    bannedWords.add(regex);
                }
            }

        } catch (Exception e) {
            Log.e("Censorship", "Error loading banned words", e);
        }
    }

    private void getCurrentUsername(OnUsernameFetchedListener listener) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            listener.onUsernameFetched("Anonymous");
            return;
        }

        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String uname = documentSnapshot.getString("username");
                        listener.onUsernameFetched(uname != null ? uname : "Anonymous");
                    } else {
                        listener.onUsernameFetched("Anonymous");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PostDetailActivity", "Error fetching username", e);
                    listener.onUsernameFetched("Anonymous");
                });
    }

    interface OnUsernameFetchedListener {
        void onUsernameFetched(String username);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_post_detail);

        rootView = findViewById(android.R.id.content);
        ivDetailImage = findViewById(R.id.ivDetailImage);

        TextView tvBack = findViewById(R.id.tvBack);
        tvBack.setOnClickListener(v -> finish());

        this.imageUrl = getIntent().getStringExtra("imageUrl");

        ivDetailImage.setOnClickListener(v -> {
            Intent intent = new Intent(PostDetailActivity.this, ImageFullView.class);
            intent.putExtra("imageUrl", this.imageUrl);
            startActivity(intent);
        });

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        ImageButton  fab = findViewById(R.id.fab);

        fab.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private int lastAction;
            private int screenWidth, screenHeight;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (screenWidth == 0 || screenHeight == 0) {
                    screenWidth = ((View) view.getParent()).getWidth();
                    screenHeight = ((View) view.getParent()).getHeight();
                }

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        lastAction = MotionEvent.ACTION_DOWN;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        newY = Math.max(0, Math.min(newY, screenHeight - view.getHeight()));

                        view.setX(newX);
                        view.setY(newY);
                        lastAction = MotionEvent.ACTION_MOVE;
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            fab.setOnTouchListener(new View.OnTouchListener() {
                                private float dX, dY;
                                private int lastAction;
                                private int screenWidth, screenHeight;

                                @Override
                                public boolean onTouch(View view, MotionEvent event) {
                                    if (screenWidth == 0 || screenHeight == 0) {
                                        screenWidth = ((View) view.getParent()).getWidth();
                                        screenHeight = ((View) view.getParent()).getHeight();
                                    }

                                    switch (event.getActionMasked()) {
                                        case MotionEvent.ACTION_DOWN:
                                            dX = view.getX() - event.getRawX();
                                            dY = view.getY() - event.getRawY();
                                            lastAction = MotionEvent.ACTION_DOWN;
                                            return true;

                                        case MotionEvent.ACTION_MOVE:
                                            float newX = event.getRawX() + dX;
                                            float newY = event.getRawY() + dY;
                                            newY = Math.max(0, Math.min(newY, screenHeight - view.getHeight()));
                                            view.setX(newX);
                                            view.setY(newY);
                                            lastAction = MotionEvent.ACTION_MOVE;
                                            return true;

                                        case MotionEvent.ACTION_UP:
                                            if (lastAction == MotionEvent.ACTION_DOWN) {

                                                showNoteBottomSheet();
                                            } else {
                                                float middle = screenWidth / 2f;
                                                float targetX = (view.getX() < middle) ? 0 : screenWidth - view.getWidth();
                                                view.animate().x(targetX).setDuration(200).start();
                                            }
                                            return true;

                                        default:
                                            return false;
                                    }
                                }
                            });

                        } else {
                            float middle = screenWidth / 2f;
                            float targetX = (view.getX() < middle) ? 0 : screenWidth - view.getWidth();

                            view.animate()
                                    .x(targetX)
                                    .setDuration(200)
                                    .start();
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });

        Intent intent = getIntent();
        if (intent == null) {
            showCustomToast("Error loading post details");
            finish();
            return;
        }

        imageUrl = intent.getStringExtra("imageUrl");
        String mushroomType = intent.getStringExtra("mushroomType");
        String description = intent.getStringExtra("description");
        username = intent.getStringExtra("username");
        postId = intent.getStringExtra("postId");
        postAuthorId = intent.getStringExtra("userId");
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);

        if (postId == null || postId.isEmpty()) {
            showCustomToast("Post ID missing");
            finish();
            return;
        }

        ImageView ivDetailImage = findViewById(R.id.ivDetailImage);
        TextView tvDetailType = findViewById(R.id.tvDetailType);
        TextView tvDetailDesc = findViewById(R.id.tvDetailDesc);
        TextView tvDetailUser = findViewById(R.id.tvDetailUser);
        miniMapView = findViewById(R.id.miniMapView);
        btnUpvote = findViewById(R.id.btnUpvote);
        btnDownvote = findViewById(R.id.btnDownvote);
        tvUpvotes = findViewById(R.id.tvUpvotes);
        tvDownvotes = findViewById(R.id.tvDownvotes);
        ivMenuOptions = findViewById(R.id.ivMenuOptions);
        TextView tvCategory = findViewById(R.id.tvCategory);
        TextView tvLocation = findViewById(R.id.tvLocation);
        TextView tvTimestamp = findViewById(R.id.tvTimestamp);
        TextView tvVerified = findViewById(R.id.tvVerified);
        commentsCount = findViewById(R.id.commentsCount);
        LinearLayout correctedContainer = findViewById(R.id.correctedContainer);
        TextView tvVerifiedBy = findViewById(R.id.tvVerifiedBy);
        ImageView imageVerifiedBadge = findViewById(R.id.imageVerifiedBadge);
        TextView tvCorrected = findViewById(R.id.tvCorrected);
        TextView tvCorrectedCategory = findViewById(R.id.tvCorrectedCategory);


        tvDetailType.setText(mushroomType != null ? mushroomType : "Unknown Type");
        tvDetailDesc.setText(description != null ? description : "No description available");
        tvDetailUser.setText((username != null ? username : "Unknown"));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(ivDetailImage);
        }

        db.collection("posts").document(postId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        String postDescription = documentSnapshot.getString("description");
                        tvDetailDesc.setText(postDescription != null && !postDescription.isEmpty()
                                ? postDescription
                                : "No description available");

                        String category = documentSnapshot.getString("category");
                        if (category == null || category.isEmpty()) {
                            category = "Uncategorized";
                        }
                        tvCategory.setText(category);

                        GradientDrawable bg = new GradientDrawable();
                        bg.setCornerRadius(5 * getResources().getDisplayMetrics().density);

                        switch (category.toLowerCase()) {
                            case "edible":
                                bg.setColor(getResources().getColor(android.R.color.holo_green_light));
                                tvCategory.setTextColor(getResources().getColor(android.R.color.white));
                                break;
                            case "poisonous":
                                bg.setColor(getResources().getColor(android.R.color.holo_red_light));
                                tvCategory.setTextColor(getResources().getColor(android.R.color.white));
                                break;
                            case "inedible":
                                bg.setColor(0xFFFFA500);
                                tvCategory.setTextColor(getResources().getColor(android.R.color.white));
                                break;
                            case "medicinal":
                                bg.setColor(getResources().getColor(android.R.color.holo_blue_light));
                                tvCategory.setTextColor(getResources().getColor(android.R.color.white));
                                break;
                            default:
                                bg.setColor(getResources().getColor(android.R.color.darker_gray));
                                tvCategory.setTextColor(getResources().getColor(android.R.color.white));
                                break;
                        }

                        tvCategory.setBackground(bg);

                        String location = documentSnapshot.getString("location");
                        tvLocation.setText((location != null ? location : "Unknown"));

                        Object rawTimestamp = documentSnapshot.get("timestamp");
                        if (rawTimestamp instanceof com.google.firebase.Timestamp) {
                            long millis = ((com.google.firebase.Timestamp) rawTimestamp).toDate().getTime();
                            String formattedDate = new java.text.SimpleDateFormat(
                                    "MMM dd, yyyy HH:mm", java.util.Locale.getDefault()
                            ).format(new java.util.Date(millis));
                            tvTimestamp.setText(formattedDate);
                        } else if (rawTimestamp instanceof Long) {
                            String formattedDate = new java.text.SimpleDateFormat(
                                    "MMM dd, yyyy HH:mm", java.util.Locale.getDefault()
                            ).format(new java.util.Date((Long) rawTimestamp));
                            tvTimestamp.setText(formattedDate);
                        } else {
                            tvTimestamp.setText("Posted on: Unknown");
                        }

                        String verified = documentSnapshot.getString("verified");
                        if (verified == null || verified.isEmpty()) {
                            verified = "Pending review";
                        }
                        tvVerified.setText("● " + verified);

                        if ("Verified".equalsIgnoreCase(verified)) {
                            tvVerified.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        } else if ("Rejected".equalsIgnoreCase(verified)) {
                            tvVerified.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        } else {
                            tvVerified.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        }
                    }

                    String verifiedStatus = documentSnapshot.getString("verified");
                    if ("Unreliable".equalsIgnoreCase(verifiedStatus)) {
                        correctedContainer.setVisibility(View.VISIBLE);

                        String verifiedBy = documentSnapshot.getString("verified_by");
                        String correction = documentSnapshot.getString("correction");
                        String correctCategory = documentSnapshot.getString("correct_category");

                        tvVerifiedBy.setText(verifiedBy != null ? verifiedBy : "Unknown Admin");
                        tvCorrected.setText(correction != null ? correction : "No correction provided");
                        tvCorrectedCategory.setText(
                                correctCategory != null ? correctCategory : "Unknown"
                        );

                        GradientDrawable bg = new GradientDrawable();
                        bg.setCornerRadius(5 * getResources().getDisplayMetrics().density);

                        if (correctCategory != null) {
                            switch (correctCategory.toLowerCase()) {
                                case "edible":
                                    bg.setColor(getResources().getColor(android.R.color.holo_green_light));
                                    tvCorrectedCategory.setTextColor(getResources().getColor(android.R.color.white));
                                    break;
                                case "poisonous":
                                    bg.setColor(getResources().getColor(android.R.color.holo_red_light));
                                    tvCorrectedCategory.setTextColor(getResources().getColor(android.R.color.white));
                                    break;
                                case "inedible":
                                case "inedible (non-toxic)":
                                    bg.setColor(0xFFFFA500);
                                    tvCorrectedCategory.setTextColor(getResources().getColor(android.R.color.white));
                                    break;
                                case "medicinal":
                                    bg.setColor(getResources().getColor(android.R.color.holo_blue_light));
                                    tvCorrectedCategory.setTextColor(getResources().getColor(android.R.color.white));
                                    break;
                                default:
                                    bg.setColor(getResources().getColor(android.R.color.darker_gray));
                                    tvCorrectedCategory.setTextColor(getResources().getColor(android.R.color.white));
                                    break;
                            }
                        } else {
                            bg.setColor(getResources().getColor(android.R.color.darker_gray));
                            tvCorrectedCategory.setTextColor(getResources().getColor(android.R.color.white));
                        }

                        tvCorrectedCategory.setBackground(bg);
                        imageVerifiedBadge.setVisibility(View.VISIBLE);
                    } else {
                        correctedContainer.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PostDetailActivity", "Error fetching post details", e);
                    showCustomToast("Failed to load post details");
                });


        db.collection("posts").document(postId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        postAuthorId = doc.getString("userId");
                        setupMenuOptions();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PostDetailActivity", "Error fetching post author", e);
                });

        loadBannedWords();
        loadVotes();
        setupMiniMap();
        setupComments();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && postId != null) {
            String userId = currentUser.getUid();

            db.collection("posts")
                    .document(postId)
                    .collection("votes")
                    .document(userId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (snapshot != null && snapshot.exists()) {
                            String type = snapshot.getString("type");
                            if ("upvote".equals(type)) {
                                btnUpvote.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
                                btnDownvote.setColorFilter(null);
                            } else if ("downvote".equals(type)) {
                                btnDownvote.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
                                btnUpvote.setColorFilter(null);
                            }
                        } else {
                            btnUpvote.setColorFilter(null);
                            btnDownvote.setColorFilter(null);
                        }
                    });

            btnUpvote.setOnClickListener(v -> {
                db.collection("posts")
                        .document(postId)
                        .collection("votes")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists() && "upvote".equals(doc.getString("type"))) {
                                doc.getReference().delete();
                            } else {
                                doc.getReference().set(new Vote("upvote", System.currentTimeMillis()));
                            }
                        });
            });

            btnDownvote.setOnClickListener(v -> {
                db.collection("posts")
                        .document(postId)
                        .collection("votes")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists() && "downvote".equals(doc.getString("type"))) {
                                doc.getReference().delete();
                            } else {
                                doc.getReference().set(new Vote("downvote", System.currentTimeMillis()));
                            }
                        });
            });
        }
    }

    class Vote {
        public String type;
        public long timestamp;

        public Vote() {}

        public Vote(String type, long timestamp) {
            this.type = type;
            this.timestamp = timestamp;
        }
    }

    private void showNoteBottomSheet() {
        if (rootView == null || isFinishing()) return;

        View sheetView = getLayoutInflater().inflate(R.layout.bottomsheet_note, null);
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        bottomSheetDialog.setContentView(sheetView);

        EditText etTitle = sheetView.findViewById(R.id.etDialogTitleInput);
        EditText etNote = sheetView.findViewById(R.id.etDialogNote);
        Button btnSave = sheetView.findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String text = etNote.getText().toString().trim();

            if (title.isEmpty() || text.isEmpty()) {
                showCustomToast("Title and note cannot be empty");
                return;
            }

            saveNoteToFirebase(title, text);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void saveNoteToFirebase(String title, String text) {
        if (currentUserId == null) return;

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("title", title);
        noteData.put("text", text);
        noteData.put("date", com.google.firebase.Timestamp.now());

        db.collection("users")
                .document(currentUserId)
                .collection("notes")
                .add(noteData)
                .addOnSuccessListener(docRef -> {
                    showCustomToast("Note saved!");
                    Log.d("PostDetailActivity", "Note saved with ID: " + docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("PostDetailActivity", "Failed to save note", e);
                    showCustomToast("Failed to save note");
                });

    }

    private void setupMenuOptions() {
        if (rootView == null || isFinishing()) return;

        ivMenuOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, ivMenuOptions);
            popup.getMenuInflater().inflate(R.menu.menu_post_options, popup.getMenu());

            if (isPostOwnedByCurrentUser()) {
                popup.getMenu().findItem(R.id.menu_delete).setVisible(true);
                popup.getMenu().findItem(R.id.menu_report).setVisible(false);
            } else {
                popup.getMenu().findItem(R.id.menu_delete).setVisible(false);
                popup.getMenu().findItem(R.id.menu_report).setVisible(true);
            }

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_delete) {
                    showDeleteConfirmationDialog();
                    return true;
                } else if (itemId == R.id.menu_report) {
                    showReportDialog();
                    return true;
                }
                return false;
            });

            popup.show();
        });
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.toast_root));

        TextView toastText = layout.findViewById(R.id.toast_text);
        toastText.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }


    private boolean isPostOwnedByCurrentUser() {

        if (postAuthorId != null && currentUserId != null) {
            return postAuthorId.equals(currentUserId);
        }

        return false;
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePost())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePost() {
        if (postId == null || currentUserId == null) {
            showCustomToast("Unable to delete post");
            return;
        }

        db.collection("posts").document(postId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    showCustomToast("Post deleted successfully");
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("PostDetailActivity", "Error deleting post", e);
                    showCustomToast("Failed to delete post");
                });

    }

    private void showReportDialog() {
        String[] reportReasons = {
                "Inappropriate content",
                "Spam",
                "Harassment",
                "False information",
                "Other"
        };

        new AlertDialog.Builder(this)
                .setTitle("Report Post")
                .setItems(reportReasons, (dialog, which) -> {
                    String reason = reportReasons[which];
                    submitReport(reason);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitReport(String reason) {
        if (currentUserId == null) {
            showCustomToast("Login required to report");
            return;
        }

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("postId", postId);
        reportData.put("reportedBy", currentUserId);
        reportData.put("reason", reason);
        reportData.put("timestamp", FieldValue.serverTimestamp());
        reportData.put("postAuthor", postAuthorId);

        db.collection("reports")
                .add(reportData)
                .addOnSuccessListener(documentReference -> {
                    showCustomToast("Report submitted successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("PostDetailActivity", "Error submitting report", e);
                    showCustomToast("Failed to submit report");
                });

    }

    private String censorIfNeeded(String input) {
        if (input == null || input.isEmpty()) return input;

        String normalized = input.toLowerCase().replaceAll("[^a-z]", "");

        for (String regex : bannedWords) {
            if (normalized.matches(".*" + regex + ".*")) {
                return input.replaceAll(".", "*");
            }
        }

        return input;
    }

    private void loadVotes() {
        db.collection("posts").document(postId)
                .collection("votes")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot != null) {
                        int upvotes = 0;
                        int downvotes = 0;

                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot) {
                            String type = doc.getString("type");
                            if ("upvote".equals(type)) upvotes++;
                            else if ("downvote".equals(type)) downvotes++;
                        }

                        tvUpvotes.setText("+" + upvotes);
                        tvDownvotes.setText("-" + downvotes);

                        if (currentUserId != null) {
                            currentUserVote = null;
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                                if (doc.getId().equals(currentUserId)) {
                                    currentUserVote = doc.getString("type");
                                    break;
                                }
                            }
                            updateVoteUI(currentUserVote);
                        } else {
                            updateVoteUI(null);
                        }
                    }
                });
    }

    private void updateVote(String voteType) {
        if (currentUserId == null) {
            showCustomToast("Login required to vote");
            return;
        }

        if (voteType.equals(currentUserVote)) {
            db.collection("posts").document(postId)
                    .collection("votes").document(currentUserId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        currentUserVote = null;
                        updateVoteUI(null);
                    })
                    .addOnFailureListener(e ->
                            showCustomToast("Failed to remove vote")
                    );
        } else {
            Map<String, Object> voteData = new HashMap<>();
            voteData.put("type", voteType);
            voteData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

            db.collection("posts").document(postId)
                    .collection("votes").document(currentUserId)
                    .set(voteData)
                    .addOnSuccessListener(aVoid -> {
                        currentUserVote = voteType;
                        updateVoteUI(voteType);
                    })
                    .addOnFailureListener(e ->
                            showCustomToast("Failed to vote")

                    );
        }
    }

    private void updateVoteUI(String userVote) {
        int selectedUp = getResources().getColor(android.R.color.holo_green_dark);
        int selectedDown = getResources().getColor(android.R.color.holo_red_dark);
        int unselected = getResources().getColor(android.R.color.darker_gray);

        if ("upvote".equals(userVote)) {
            btnUpvote.setColorFilter(selectedUp);
            btnDownvote.setColorFilter(unselected);
        } else if ("downvote".equals(userVote)) {
            btnUpvote.setColorFilter(unselected);
            btnDownvote.setColorFilter(selectedDown);
        } else {
            btnUpvote.setColorFilter(unselected);
            btnDownvote.setColorFilter(unselected);
        }
    }


    private void setupMiniMap() {
        if (rootView == null || isFinishing()) return;
        try {
            miniMapView.setMultiTouchControls(false);
            miniMapView.setClickable(false);
            miniMapView.getController().setZoom(15.0);

            if (latitude != 0.0 && longitude != 0.0) {
                GeoPoint postLocation = new GeoPoint(latitude, longitude);
                miniMapView.getController().setCenter(postLocation);

                Marker marker = new Marker(miniMapView);
                marker.setPosition(postLocation);
                marker.setTitle("Post Location");
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                miniMapView.getOverlays().add(marker);
                miniMapView.invalidate();

                final String markerImageUrl = this.imageUrl;

                if (markerImageUrl != null && !markerImageUrl.isEmpty()) {
                    Log.d("PostDetailActivity", "Loading marker image: " + markerImageUrl);

                    Glide.with(this)
                            .asBitmap()
                            .load(markerImageUrl)
                            .override(50, 50)
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                    try {

                                        int size = (int) (30 * getResources().getDisplayMetrics().density);
                                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(resource, size, size, true);

                                        Bitmap circularBitmap = createCircularBitmap(scaledBitmap);

                                        marker.setIcon(new android.graphics.drawable.BitmapDrawable(getResources(), circularBitmap));
                                        miniMapView.invalidate();

                                        Log.d("PostDetailActivity", "Custom marker loaded successfully");
                                    } catch (Exception e) {
                                        Log.e("PostDetailActivity", "Error creating custom marker", e);
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {

                                }

                                @Override
                                public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                                    Log.e("PostDetailActivity", "Failed to load marker image: " + markerImageUrl);

                                }
                            });
                }

                miniMapView.setOnClickListener(v -> openFullMap());

                TextView btnOpenFullMap = findViewById(R.id.btnOpenFullMap);
                if (btnOpenFullMap != null) {
                    btnOpenFullMap.setOnClickListener(v -> openFullMap());
                }
            }

        } catch (Exception e) {
            Log.e("PostDetailActivity", "Error setting up mini map", e);
        }
    }

    private Bitmap createCircularBitmap(Bitmap bitmap) {
        int size = bitmap.getWidth();
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);

        final android.graphics.Paint paint = new android.graphics.Paint();
        final android.graphics.Rect rect = new android.graphics.Rect(0, 0, size, size);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        paint.setXfermode(null);
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint);

        return output;
    }

    private void openFullMap() {
        Intent intent = new Intent(PostDetailActivity.this, FullMap.class);
        intent.putExtra("postId", postId);
        startActivity(intent);
    }

    private void setupComments() {
        if (rootView == null || isFinishing()) return;

        rvComments = findViewById(R.id.rvComments);
        etComment = findViewById(R.id.etComment);
        btnPostComment = findViewById(R.id.btnPostComment);

        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((QuerySnapshot snapshot, FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Log.e("PostDetailActivity", "Error listening to comments", e);
                        return;
                    }

                    commentList.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            try {
                                Comment comment = doc.toObject(Comment.class);
                                commentList.add(comment);
                            } catch (Exception ex) {
                                Log.e("PostDetailActivity", "Error parsing comment", ex);
                            }
                        }
                        commentAdapter.notifyDataSetChanged();
                    }
                });

        db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((QuerySnapshot snapshot, FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Log.e("PostDetailActivity", "Error listening to comments", e);
                        return;
                    }

                    commentList.clear();
                    int commentCount = 0;

                    if (snapshot != null) {
                        commentCount = snapshot.size();

                        for (QueryDocumentSnapshot doc : snapshot) {
                            try {
                                Comment comment = doc.toObject(Comment.class);
                                commentList.add(comment);
                            } catch (Exception ex) {
                                Log.e("PostDetailActivity", "Error parsing comment", ex);
                            }
                        }
                        commentAdapter.notifyDataSetChanged();
                    }

                    commentsCount.setText("(" + commentCount + ")");
                });


        btnPostComment.setOnClickListener(v -> postComment());
    }

    private void postComment() {
        if (rootView == null || isFinishing()) return;

        String rawText = etComment.getText().toString().trim();
        if (rawText.isEmpty()) {
            showCustomToast("Please enter a comment");
            return;
        }

        final String text = censorIfNeeded(rawText);

        btnPostComment.setEnabled(false);

        getCurrentUsername(currentUsername -> {
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("user", currentUsername);
            commentData.put("text", text);
            commentData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

            db.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .add(commentData)
                    .addOnSuccessListener(documentReference -> {
                        etComment.setText("");
                        btnPostComment.setEnabled(true);
                        Log.d("PostDetailActivity", "Comment posted successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("PostDetailActivity", "Error posting comment", e);
                        showCustomToast("Failed to post comment");
                        btnPostComment.setEnabled(true);
                    });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (miniMapView != null) {
            miniMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniMapView != null) {
            miniMapView.onPause();
        }
    }
}

class Comment {
    private String user;
    private String text;
    private com.google.firebase.Timestamp timestamp;

    public Comment() {}

    public Comment(String user, String text) {
        this.user = user;
        this.text = text;
        this.timestamp = com.google.firebase.Timestamp.now();
    }

    public String getUser() { return user; }
    public String getText() { return text; }
    public com.google.firebase.Timestamp getTimestamp() { return timestamp; }

    public void setUser(String user) { this.user = user; }
    public void setText(String text) { this.text = text; }
    public void setTimestamp(com.google.firebase.Timestamp timestamp) { this.timestamp = timestamp; }
}

class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.user.setText(comment.getUser() != null ? comment.getUser() : "Anonymous");
        holder.text.setText(comment.getText() != null ? comment.getText() : "");

        if (comment.getTimestamp() != null) {
            long timestampMillis = comment.getTimestamp().toDate().getTime();
            String timeAgo = formatTimeAgo(timestampMillis);
            holder.timestamp.setText(timeAgo);
        } else {
            holder.timestamp.setText("");
        }
    }

    private String formatTimeAgo(long timestampMillis) {
        long now = System.currentTimeMillis();
        long diff = now - timestampMillis;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (minutes < 1) {
            return "1 minute ago";
        } else if (hours < 1) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else if (days <= 5) {
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        } else {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
            return dateFormat.format(new java.util.Date(timestampMillis));
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView user, text, timestamp;

        CommentViewHolder(View itemView) {
            super(itemView);
            user = itemView.findViewById(R.id.tvCommentUser);
            text = itemView.findViewById(R.id.tvCommentText);
            timestamp = itemView.findViewById(R.id.tvCommentTimestamp);
        }
    }
}



