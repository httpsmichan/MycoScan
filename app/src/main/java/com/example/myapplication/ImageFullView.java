package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ImageFullView extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tvImageCounter;
    private FirebaseFirestore db;
    private String postId;
    private List<String> imageUrls = new ArrayList<>();
    private ImagePagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_full_view);

        db = FirebaseFirestore.getInstance();

        TextView tvBack = findViewById(R.id.tvBack);
        tvBack.setOnClickListener(v -> finish());

        viewPager = findViewById(R.id.viewPager);
        tvImageCounter = findViewById(R.id.tvImageCounter);

        // Get data from intent
        String imageUrl = getIntent().getStringExtra("imageUrl");
        postId = getIntent().getStringExtra("postId");

        if (postId != null && !postId.isEmpty()) {
            // Fetch images from Firestore
            fetchImagesFromFirestore();
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            // Fallback: display single image if postId not provided
            imageUrls.add(imageUrl);
            setupViewPager();
        }
    }

    private void fetchImagesFromFirestore() {
        db.collection("posts")
                .document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Check if imageUrl is a list or single string
                        Object imageUrlObj = documentSnapshot.get("imageUrl");

                        if (imageUrlObj instanceof List) {
                            // Multiple images
                            List<?> urlList = (List<?>) imageUrlObj;
                            for (Object url : urlList) {
                                if (url instanceof String) {
                                    imageUrls.add((String) url);
                                }
                            }
                        } else if (imageUrlObj instanceof String) {
                            // Single image
                            imageUrls.add((String) imageUrlObj);
                        }

                        if (!imageUrls.isEmpty()) {
                            setupViewPager();
                        } else {
                            Log.e("ImageFullView", "No images found");
                            finish();
                        }
                    } else {
                        Log.e("ImageFullView", "Document does not exist");
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ImageFullView", "Error fetching images", e);
                    finish();
                });
    }

    private void setupViewPager() {
        adapter = new ImagePagerAdapter(imageUrls, this);
        viewPager.setAdapter(adapter);

        // Update counter
        updateImageCounter(0);

        // Listen to page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateImageCounter(position);
            }
        });

        // Show counter only if there are multiple images
        if (imageUrls.size() > 1) {
            tvImageCounter.setVisibility(TextView.VISIBLE);
        } else {
            tvImageCounter.setVisibility(TextView.GONE);
        }
    }

    private void updateImageCounter(int position) {
        if (imageUrls.size() > 1) {
            tvImageCounter.setText((position + 1) + " / " + imageUrls.size());
        }
    }
}