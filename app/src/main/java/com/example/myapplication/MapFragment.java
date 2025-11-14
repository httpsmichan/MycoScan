package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private MapView mapView;
    private Polygon davaoPolygon;
    private EditText searchBar;
    private Button filterAll, filterEdible, filterPoisonous, filterUnknown, filterByUser, filterInedible;

    private List<DocumentSnapshot> allPosts = new ArrayList<>();
    private String currentFilter = "All";
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.activity_map, container, false);

        mapView = root.findViewById(R.id.mapView);
        searchBar = root.findViewById(R.id.searchBar);
        filterAll = root.findViewById(R.id.filterAll);
        filterEdible = root.findViewById(R.id.filterEdible);
        filterPoisonous = root.findViewById(R.id.filterPoisonous);
        filterUnknown = root.findViewById(R.id.filterUnknown);
        filterByUser = root.findViewById(R.id.filterByUser);
        filterInedible = root.findViewById(R.id.filterInedible);


        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        drawDavaoBoundary();
        setupFilters();
        setupSearch();
        loadPostLocations();

        return root;
    }

    private void setupFilters() {
        filterAll.setOnClickListener(v -> {
            currentFilter = "All";
            updateFilterButtons();
            applyFilters();
        });

        filterEdible.setOnClickListener(v -> {
            currentFilter = "Edible";
            updateFilterButtons();
            applyFilters();
        });

        filterInedible.setOnClickListener(v -> {
            currentFilter = "Inedible";
            updateFilterButtons();
            applyFilters();
        });

        filterPoisonous.setOnClickListener(v -> {
            currentFilter = "Poisonous";
            updateFilterButtons();
            applyFilters();
        });

        filterUnknown.setOnClickListener(v -> {
            currentFilter = "Unknown / Needs ID";
            updateFilterButtons();
            applyFilters();
        });

        filterByUser.setOnClickListener(v -> {
            showUserFilterDialog();
        });

        updateFilterButtons();
    }

    private void updateFilterButtons() {

        filterAll.setBackgroundColor(Color.LTGRAY);
        filterEdible.setBackgroundColor(Color.LTGRAY);
        filterPoisonous.setBackgroundColor(Color.LTGRAY);
        filterUnknown.setBackgroundColor(Color.LTGRAY);
        filterByUser.setBackgroundColor(Color.LTGRAY);
        filterInedible.setBackgroundColor(Color.LTGRAY);

        if (currentFilter.equals("All")) {
            filterAll.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else if (currentFilter.equals("Edible")) {
            filterEdible.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else if (currentFilter.equals("Inedible")) {
            filterInedible.setBackgroundColor(Color.parseColor("#FF9800"));
        }else if (currentFilter.equals("Poisonous")) {
            filterPoisonous.setBackgroundColor(Color.parseColor("#F44336"));
        } else if (currentFilter.equals("Unknown / Needs ID")) {
            filterUnknown.setBackgroundColor(Color.parseColor("#9E9E9E"));
        } else {
            filterByUser.setBackgroundColor(Color.parseColor("#2196F3"));
        }
    }

    private void showUserFilterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Filter by Username");

        final EditText input = new EditText(requireContext());
        input.setHint("Enter username");
        builder.setView(input);

        builder.setPositiveButton("Filter", (dialog, which) -> {
            String username = input.getText().toString().trim();
            if (!username.isEmpty()) {
                currentFilter = "User:" + username;
                updateFilterButtons();
                applyFilters();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadPostLocations() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference postsRef = db.collection("posts");

        postsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            allPosts.clear();
            allPosts.addAll(queryDocumentSnapshots.getDocuments());
            applyFilters();

            mapView.getController().setZoom(11.0);
            mapView.getController().setCenter(new GeoPoint(7.0731, 125.6088));
        });
    }

    // Helper method to get first image URL from imageUrl field (array or string)
    private String getFirstImageUrl(Object imageObj) {
        if (imageObj instanceof String) {
            return (String) imageObj;
        } else if (imageObj instanceof List) {
            List<?> imageList = (List<?>) imageObj;
            if (!imageList.isEmpty() && imageList.get(0) instanceof String) {
                return (String) imageList.get(0);
            }
        }
        return null;
    }

    // Create a circular marker icon with image and border
    private Bitmap createCircularMarkerWithBorder(Bitmap imageBitmap, int size) {
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // Draw white circle border
        Paint borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, borderPaint);

        // Draw image circle (slightly smaller to show border)
        Paint imagePaint = new Paint();
        imagePaint.setAntiAlias(true);

        int imageSize = size - 8; // 4px border on each side
        Bitmap scaledImage = Bitmap.createScaledBitmap(imageBitmap, imageSize, imageSize, true);

        // Clip to circle
        canvas.save();
        android.graphics.Path clipPath = new android.graphics.Path();
        clipPath.addCircle(size / 2f, size / 2f, imageSize / 2f, android.graphics.Path.Direction.CW);
        canvas.clipPath(clipPath);
        canvas.drawBitmap(scaledImage, (size - imageSize) / 2f, (size - imageSize) / 2f, imagePaint);
        canvas.restore();

        return output;
    }

    private void applyFilters() {
        if (!isAdded() || mapView == null) return;

        mapView.getOverlays().clear();
        mapView.getOverlays().add(davaoPolygon);

        for (DocumentSnapshot doc : allPosts) {
            Double lat = doc.getDouble("latitude");
            Double lon = doc.getDouble("longitude");
            if (lat == null || lon == null || !DavaoGeoFence.isInsideDavao(lat, lon)) continue;

            String type = doc.getString("mushroomType");
            String user = doc.getString("username");
            String category = doc.getString("category");
            Object imageObj = doc.get("imageUrl");
            String imageUrl = getFirstImageUrl(imageObj);

            String postId = doc.getId();

            if (!currentFilter.equals("All")) {
                if (currentFilter.startsWith("User:")) {
                    String filterUsername = currentFilter.substring(5);
                    if (user == null || !user.equalsIgnoreCase(filterUsername)) continue;
                } else if (category == null || !category.equals(currentFilter)) continue;
            }
            if (!searchQuery.isEmpty()) {
                boolean matchFound = false;
                if (type != null && type.toLowerCase().contains(searchQuery)) matchFound = true;
                if (user != null && user.toLowerCase().contains(searchQuery)) matchFound = true;
                if (!matchFound) continue;
            }

            if (mapView == null) return;

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(lat, lon));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            marker.setTitle((type != null ? type : "Post") + (user != null ? "\nby " + user : ""));
            marker.setRelatedObject(postId);

            marker.setOnMarkerClickListener((m, mv) -> {
                showPostDetails(doc);
                return true;
            });

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(requireContext())
                        .asBitmap()
                        .load(imageUrl)
                        .circleCrop()
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource,
                                                        @Nullable Transition<? super Bitmap> transition) {
                                if (!isAdded() || mapView == null) return;
                                int size = 70;
                                Bitmap circularMarker = createCircularMarkerWithBorder(resource, size);
                                marker.setIcon(new BitmapDrawable(requireContext().getResources(), circularMarker));
                                mapView.invalidate();
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {}
                        });
            }

            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();
    }

    private void showPostDetails(DocumentSnapshot doc) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_post_details, null);
        bottomSheet.setContentView(sheetView);

        ImageView postImage = sheetView.findViewById(R.id.postDetailImage);
        TextView mushroomType = sheetView.findViewById(R.id.postDetailMushroomType);
        TextView category = sheetView.findViewById(R.id.postDetailCategory);
        TextView username = sheetView.findViewById(R.id.postDetailUsername);
        TextView verifiedBadge = sheetView.findViewById(R.id.postDetailVerified);
        TextView description = sheetView.findViewById(R.id.postDetailDescription);
        TextView location = sheetView.findViewById(R.id.postDetailLocation);
        TextView reportBtn = sheetView.findViewById(R.id.btnReportPost);

        Object imageObj = doc.get("imageUrl");
        String imageUrl = getFirstImageUrl(imageObj);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext()).load(imageUrl).into(postImage);
        }

        mushroomType.setText(doc.getString("mushroomType"));

        String cat = doc.getString("category");
        if (cat != null) {
            category.setText(cat);
            category.setVisibility(View.VISIBLE);
            if (cat.equals("Edible")) category.setBackgroundColor(Color.parseColor("#4CAF50"));
            else if (cat.equals("Poisonous")) category.setBackgroundColor(Color.parseColor("#F44336"));
            else if (cat.equals("Unknown / Needs ID")) category.setBackgroundColor(Color.parseColor("#9E9E9E"));
        } else category.setVisibility(View.GONE);

        username.setText(doc.getString("username"));

        String verified = doc.getString("verified");
        verifiedBadge.setVisibility(verified != null && verified.equals("verified") ? View.VISIBLE : View.GONE);

        String desc = doc.getString("description");
        if (desc != null && !desc.isEmpty()) {
            description.setText(desc);
            description.setVisibility(View.VISIBLE);
        } else description.setVisibility(View.GONE);

        String loc = doc.getString("location");
        if (loc != null) location.setText("📍 " + loc);

        reportBtn.setOnClickListener(v -> {
            reportPost(doc.getId());
        });

        bottomSheet.show();
    }

    private void reportPost(String postId) {
        if (postId == null) {
            showCustomToast("Cannot report this post");
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_report, null);

        EditText etProblem = dialogView.findViewById(R.id.etProblem);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String reason = etProblem.getText().toString().trim();
            if (reason.isEmpty()) {
                showCustomToast("Please enter something");
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            java.util.Map<String, Object> report = new java.util.HashMap<>();
            report.put("postId", postId);
            report.put("reason", reason);
            report.put("timestamp", System.currentTimeMillis());
            report.put("reportedBy", com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid());

            db.collection("reports")
                    .add(report)
                    .addOnSuccessListener(docRef -> {
                        showCustomToast("Report submitted. Thank you!");
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            showCustomToast("Failed to submit report")
                    );
        });

        dialog.show();
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


    private void drawDavaoBoundary() {
        davaoPolygon = new Polygon();
        davaoPolygon.setPoints(DavaoGeoFence.getBoundary());
        davaoPolygon.setStrokeColor(Color.BLUE);
        davaoPolygon.setFillColor(Color.TRANSPARENT);
        davaoPolygon.setStrokeWidth(2f);
        mapView.getOverlays().add(davaoPolygon);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}