package com.example.myapplication;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class FullMap extends AppCompatActivity {

    private MapView mapView;
    private FirebaseFirestore db;
    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_full_map);

        mapView = findViewById(R.id.fullMapView);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(17.0);

        db = FirebaseFirestore.getInstance();

        postId = getIntent().getStringExtra("postId");

        if (postId != null && !postId.isEmpty()) {

            fetchPostLocation(postId);
        } else {

            double lat = getIntent().getDoubleExtra("latitude", 0.0);
            double lon = getIntent().getDoubleExtra("longitude", 0.0);
            String imageUriString = getIntent().getStringExtra("imageUri");

            if (lat != 0.0 && lon != 0.0) {
                GeoPoint point = new GeoPoint(lat, lon);
                mapView.getController().setCenter(point);

                if (imageUriString != null && !imageUriString.isEmpty()) {
                    Uri imageUri = Uri.parse(imageUriString);
                    Glide.with(this)
                            .asBitmap()
                            .load(imageUri)
                            .circleCrop()
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                    int size = 80;
                                    Bitmap smallBitmap = Bitmap.createScaledBitmap(resource, size, size, false);

                                    Marker marker = new Marker(mapView);
                                    marker.setPosition(point);
                                    marker.setIcon(new android.graphics.drawable.BitmapDrawable(getResources(), smallBitmap));
                                    marker.setTitle("Your Current Location");

                                    mapView.getOverlays().add(marker);
                                    mapView.invalidate();
                                }

                                @Override
                                public void onLoadCleared(@NonNull android.graphics.drawable.Drawable placeholder) {}
                            });
                } else {
                    Marker marker = new Marker(mapView);
                    marker.setPosition(point);
                    marker.setTitle("Your Current Location");
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    mapView.getOverlays().add(marker);
                }
            } else {
                Log.e("FullMap", "No postId or coordinates received!");
            }
        }
    }

    private void fetchPostLocation(String postId) {
        db.collection("posts").document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double lat = documentSnapshot.getDouble("latitude");
                        Double lng = documentSnapshot.getDouble("longitude");
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        if (lat != null && lng != null && imageUrl != null && !imageUrl.isEmpty()) {
                            GeoPoint point = new GeoPoint(lat, lng);
                            mapView.getController().setCenter(point);
                            addCustomMarker(point, imageUrl);
                        } else {
                            Log.e("FullMap", "Missing latitude, longitude, or imageUrl for post: " + postId);
                        }
                    } else {
                        Log.e("FullMap", "Post not found: " + postId);
                    }
                })
                .addOnFailureListener(e -> Log.e("FullMap", "Error fetching post data", e));
    }

    private void addCustomMarker(GeoPoint point, String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        int size = 70;
                        Bitmap smallBitmap = Bitmap.createScaledBitmap(resource, size, size, false);

                        Marker marker = new Marker(mapView);
                        marker.setPosition(point);
                        marker.setIcon(new android.graphics.drawable.BitmapDrawable(getResources(), smallBitmap));
                        marker.setTitle("Post Location");

                        mapView.getOverlays().add(marker);
                        mapView.invalidate();
                    }

                    @Override
                    public void onLoadCleared(@NonNull android.graphics.drawable.Drawable placeholder) {}
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
