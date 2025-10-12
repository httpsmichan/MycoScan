package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class VerifiedAccount extends AppCompatActivity {

    private RecyclerView recyclerViewQueue, recyclerViewHistory;
    private PostsAdapter queueAdapter, historyAdapter;
    private List<Post> queueList, historyList;
    private FirebaseFirestore db;
    private TextView tvNoPosts;
    private EditText etSearch;
    private TextView spinnerText;

    private List<Post> allQueuePosts = new ArrayList<>();
    private List<Post> allHistoryPosts = new ArrayList<>();
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verified_account);

        TextView tvBack = findViewById(R.id.tvBack);
        tvBack.setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        recyclerViewQueue = findViewById(R.id.recyclerViewPosts);
        recyclerViewQueue.setLayoutManager(new LinearLayoutManager(this));
        queueList = new ArrayList<>();
        queueAdapter = new PostsAdapter(queueList, false,
                post -> {
                },
                this::fetchPostsFromFirebase
        );
        recyclerViewQueue.setAdapter(queueAdapter);

        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();
        historyAdapter = new PostsAdapter(historyList, true,
                post -> {

                },
                this::fetchPostsFromFirebase
        );
        recyclerViewHistory.setAdapter(historyAdapter);

        tvNoPosts = findViewById(R.id.tvNoPosts);

        etSearch = findViewById(R.id.etSearch);
        spinnerText = findViewById(R.id.spinnerText);
        spinnerText.setText("All");
        setupSearch();
        setupFilter();

        TextView tabQueue = findViewById(R.id.tabQueue);
        TextView tabHistory = findViewById(R.id.tabHistory);
        tabQueue.setOnClickListener(v -> showQueueTab(tabQueue, tabHistory));
        tabHistory.setOnClickListener(v -> showHistoryTab(tabQueue, tabHistory));

        fetchPostsFromFirebase();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchAndFilter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilter() {
        View filterRow = (View) findViewById(R.id.spinnerText).getParent();
        filterRow.setOnClickListener(v -> {
            String[] options = {"All", "Verified", "Unreliable"};
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Filter Posts")
                    .setItems(options, (dialog, which) -> {
                        currentFilter = options[which];
                        spinnerText.setText(currentFilter);
                        applySearchAndFilter(etSearch.getText().toString());
                    })
                    .show();
        });
    }

    private void applySearchAndFilter(String query) {
        List<Post> filteredQueue = new ArrayList<>();
        List<Post> filteredHistory = new ArrayList<>();

        List<Post> filterQueue = new ArrayList<>(allQueuePosts);
        List<Post> filterHistory = new ArrayList<>();
        for (Post post : allHistoryPosts) {
            if (currentFilter.equals("All") ||
                    (currentFilter.equals("Verified") && "verified".equalsIgnoreCase(post.getVerified())) ||
                    (currentFilter.equals("Unreliable") && "unreliable".equalsIgnoreCase(post.getVerified()))) {
                filterHistory.add(post);
            }
        }

        String lowerQuery = query.toLowerCase().trim();
        if (lowerQuery.isEmpty()) {
            filteredQueue = filterQueue;
            filteredHistory = filterHistory;
        } else {
            for (Post post : filterQueue) {
                if ((post.getUsername() != null && post.getUsername().toLowerCase().contains(lowerQuery)) ||
                        (post.getMushroomType() != null && post.getMushroomType().toLowerCase().contains(lowerQuery))) {
                    filteredQueue.add(post);
                }
            }
            for (Post post : filterHistory) {
                if ((post.getUsername() != null && post.getUsername().toLowerCase().contains(lowerQuery)) ||
                        (post.getMushroomType() != null && post.getMushroomType().toLowerCase().contains(lowerQuery))) {
                    filteredHistory.add(post);
                }
            }
        }

        queueAdapter.updateList(filteredQueue);
        historyAdapter.updateList(filteredHistory);

        if (recyclerViewQueue.getVisibility() == View.VISIBLE) {
            tvNoPosts.setVisibility(filteredQueue.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showQueueTab(TextView tabQueue, TextView tabHistory) {
        recyclerViewQueue.setVisibility(View.VISIBLE);
        recyclerViewHistory.setVisibility(View.GONE);
        tabQueue.setBackgroundResource(R.drawable.selected);
        tabQueue.setTextColor(Color.WHITE);
        tabHistory.setBackgroundResource(R.drawable.not_selected);
        tabHistory.setTextColor(Color.parseColor("#047857"));
        tvNoPosts.setVisibility(queueList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showHistoryTab(TextView tabQueue, TextView tabHistory) {
        recyclerViewQueue.setVisibility(View.GONE);
        recyclerViewHistory.setVisibility(View.VISIBLE);
        tabHistory.setBackgroundResource(R.drawable.selected);
        tabHistory.setTextColor(Color.WHITE);
        tabQueue.setBackgroundResource(R.drawable.not_selected);
        tabQueue.setTextColor(Color.parseColor("#047857"));
        tvNoPosts.setVisibility(View.GONE);
    }

    private void fetchPostsFromFirebase() {
        db.collection("posts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allQueuePosts.clear();
                        allHistoryPosts.clear();
                        queueList.clear();
                        historyList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Post post = document.toObject(Post.class);
                            post.setPostId(document.getId());

                            String verified = post.getVerified();
                            if ("verified".equalsIgnoreCase(verified) || "unreliable".equalsIgnoreCase(verified)) {
                                allHistoryPosts.add(post);
                                historyList.add(post);
                            } else {
                                allQueuePosts.add(post);
                                queueList.add(post);
                            }
                        }

                        queueAdapter.notifyDataSetChanged();
                        historyAdapter.notifyDataSetChanged();
                        tvNoPosts.setVisibility(recyclerViewQueue.getVisibility() == View.VISIBLE && queueList.isEmpty() ? View.VISIBLE : View.GONE);

                    } else {
                        showCustomToast("Failed to load posts");
                    }
                });
    }

    private void showCustomToast(String message) {
        View layout = getLayoutInflater().inflate(R.layout.custom_toast, null);
        TextView toastText = layout.findViewById(R.id.toast_text);
        toastText.setText(message);
        Toast toast = new Toast(this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
