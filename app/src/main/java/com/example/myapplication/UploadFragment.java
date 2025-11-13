package com.example.myapplication;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.api.IMapController;

public class UploadFragment extends Fragment {

    private static final String TAG = "UploadFragment";
    private static final int MAX_IMAGES = 10;
    private static final int MIN_IMAGES = 5;
    private TFLiteHelper tfliteHelper;

    private EditText etMushroomType, etDescription;
    private Spinner spinnerCategory;
    private TextView btnPickImage, btnGetLocation;
    private AppCompatButton btnSubmit;
    private List<Uri> imageUris = new ArrayList<>();
    private String userLocation = "Unknown";

    private FusedLocationProviderClient fusedLocationClient;

    private static final int REQUEST_LOCATION = 101;
    private HorizontalScrollView imagePreviewScrollView;
    private LinearLayout imagePreviewContainer;
    private TextView tvLatitude, tvLongitude;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private MapView mapPreview;
    private FrameLayout geoContainer;
    private LinearLayout coordinates;
    private MapView miniMapView;
    private TextView btnOpenFullMap;

    private float detectedConfidence = 0f;
    private String detectedClass = "";

    private List<String> bannedWords = new ArrayList<>();

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        int count = Math.min(clipData.getItemCount(), MAX_IMAGES - imageUris.size());

                        for (int i = 0; i < count; i++) {
                            Uri imageUri = clipData.getItemAt(i).getUri();
                            imageUris.add(imageUri);
                        }

                        if (clipData.getItemCount() > count) {
                            showCustomToast("Maximum " + MAX_IMAGES + " images allowed. Only first " + count + " selected.");
                        }
                    }
                    else if (data.getData() != null) {
                        if (imageUris.size() < MAX_IMAGES) {
                            imageUris.add(data.getData());
                        } else {
                            showCustomToast("Maximum " + MAX_IMAGES + " images reached.");
                            return;
                        }
                    }

                    updateImagePreview();
                    showCustomToast(imageUris.size() + " image(s) selected!");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_upload, container, false);

        etMushroomType = root.findViewById(R.id.etMushroomType);
        etDescription = root.findViewById(R.id.etDescription);
        spinnerCategory = root.findViewById(R.id.spinnerCategory);
        btnPickImage = root.findViewById(R.id.btnPickMedia);
        btnGetLocation = root.findViewById(R.id.btnGetLocation);
        btnSubmit = root.findViewById(R.id.btnSubmit);
        imagePreviewScrollView = root.findViewById(R.id.imagePreviewScrollView);
        imagePreviewContainer = root.findViewById(R.id.imagePreviewContainer);
        tvLatitude = root.findViewById(R.id.tvLatitude);
        tvLongitude = root.findViewById(R.id.tvLongitude);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        geoContainer = root.findViewById(R.id.geoContainer);
        coordinates = root.findViewById(R.id.coordinates);
        miniMapView = root.findViewById(R.id.miniMapView);
        btnOpenFullMap = root.findViewById(R.id.btnOpenFullMap);

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        miniMapView.setTileSource(TileSourceFactory.MAPNIK);
        miniMapView.setMultiTouchControls(false);

        tfliteHelper = new TFLiteHelper(requireContext());

        loadBannedWords();

        String[] categories = {"Edible", "Poisonous", "Inedible (Non-toxic)", "Medicinal", "Unknown / Needs ID"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickImageLauncher.launch(intent);
        });

        btnGetLocation.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            } else {
                getUserLocation();
            }
        });

        mapPreview = root.findViewById(R.id.mapPreview);
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapPreview.setTileSource(TileSourceFactory.MAPNIK);
        mapPreview.setMultiTouchControls(true);

        if (getArguments() != null) {
            String mushroomType = getArguments().getString("mushroomType");
            String category = getArguments().getString("category");
            String description = getArguments().getString("description");
            String photoUriString = getArguments().getString("photoUri");
            double lat = getArguments().getDouble("latitude", 0.0);
            double lon = getArguments().getDouble("longitude", 0.0);

            detectedConfidence = getArguments().getFloat("confidence", 0f);
            detectedClass = getArguments().getString("detectedClass", "");

            Log.d(TAG, "===== RECEIVED ARGUMENTS =====");
            Log.d(TAG, "Confidence: " + detectedConfidence);
            Log.d(TAG, "Detected Class: " + detectedClass);
            Log.d(TAG, "Mushroom Type: " + mushroomType);
            Log.d(TAG, "==============================");

            if (mushroomType != null && !mushroomType.isEmpty()) {
                etMushroomType.setText(mushroomType);
            }

            if (category != null && !category.isEmpty()) {
                for (int i = 0; i < spinnerCategory.getCount(); i++) {
                    if (spinnerCategory.getItemAtPosition(i).toString().equals(category)) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }
            }

            if (description != null && !description.isEmpty()) {
                etDescription.setText(description);
            }

            if (photoUriString != null && !photoUriString.isEmpty()) {
                imageUris.add(Uri.parse(photoUriString));
                updateImagePreview();
            }

            if (lat != 0.0 && lon != 0.0) {
                latitude = lat;
                longitude = lon;
                tvLatitude.setText("Y: " + latitude);
                tvLongitude.setText("X: " + longitude);

                geoContainer.setVisibility(View.VISIBLE);
                coordinates.setVisibility(View.VISIBLE);
                setupMiniMap();
            }
        }

        btnSubmit.setOnClickListener(v -> {
            String mushroomType = etMushroomType.getText().toString().trim();
            if (mushroomType.isEmpty()) {
                mushroomType = "Unknown";
                etMushroomType.setText("Unknown");
            }

            String category = spinnerCategory.getSelectedItem().toString();
            String description = etDescription.getText().toString().trim();

            if (!DavaoGeoFence.isInsideDavao(latitude, longitude)) {
                showCustomToast("You're outside Davao City. Posting is only allowed inside Davao City.");
                return;
            }

            if (containsBannedWord(mushroomType) || containsBannedWord(description)) {
                getCurrentUsername(username -> {
                    Map<String, Object> log = new HashMap<>();
                    log.put("username", username);
                    log.put("datestamp", System.currentTimeMillis());
                    log.put("reason", "use of banned words");

                    FirebaseFirestore.getInstance()
                            .collection("logs")
                            .add(log)
                            .addOnSuccessListener(doc ->
                                    showCustomToast("Inappropriate words detected. Logged for review.")
                            )
                            .addOnFailureListener(e ->
                                    showCustomToast("Failed to log banned word use.")
                            );
                });
                return;
            }

            if (imageUris.size() < MIN_IMAGES) {
                showCustomToast("Please select at least " + MIN_IMAGES + " images for accurate mushroom identification.");
                return;
            }

            if (imageUris.isEmpty()) {
                showCustomToast("Please select at least four images.");
                return;
            }

            showCustomToast("Analyzing images with AI model...");
            predictMushroomFromImages();
        });

        return root;
    }

    /**
     * Predict mushroom species from multiple uploaded images
     * Uses ensemble approach: analyzes all images and picks most confident prediction
     */
    private void predictMushroomFromImages() {
        new Thread(() -> {
            try {
                List<ClassificationResult> results = new ArrayList<>();

                for (Uri imageUri : imageUris) {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            requireContext().getContentResolver(),
                            imageUri
                    );
                    ClassificationResult result = tfliteHelper.classify(bitmap);
                    results.add(result);
                    Log.d(TAG, "Image prediction: " + result.label + " (" + result.confidence + ")");
                }

                ClassificationResult bestResult = ensemblePrediction(results);

                Log.d(TAG, "===== ENSEMBLE PREDICTION =====");
                Log.d(TAG, "Final prediction: " + bestResult.label);
                Log.d(TAG, "Confidence: " + bestResult.confidence);
                Log.d(TAG, "==============================");

                detectedClass = bestResult.label;
                detectedConfidence = bestResult.confidence;

                requireActivity().runOnUiThread(() -> {
                    if (!bestResult.label.equalsIgnoreCase("Unknown")) {
                        etMushroomType.setText(bestResult.label);
                        showCustomToast("AI Detected: " + bestResult.label +
                                " (" + String.format("%.1f%%", bestResult.confidence * 100) + ")");
                    } else {
                        showCustomToast("Unable to identify mushroom with confidence. Please specify manually.");
                    }
                    fetchEdibility(bestResult.label, simplifiedEdibility -> {

                        switch (simplifiedEdibility) {
                            case "Inedible":
                                spinnerCategory.setSelection(getSpinnerIndexByValue(spinnerCategory, "Inedible (Non-toxic)"));
                                break;
                            case "Poisonous":
                                spinnerCategory.setSelection(getSpinnerIndexByValue(spinnerCategory, "Poisonous"));
                                break;
                            case "Edible":
                                spinnerCategory.setSelection(getSpinnerIndexByValue(spinnerCategory, "Edible"));
                                break;
                            default:
                                spinnerCategory.setSelection(getSpinnerIndexByValue(spinnerCategory, "Unknown / Needs ID"));
                                break;
                        }

                        showCustomToast("Uploading " + imageUris.size() + " image(s)...");
                        uploadMultipleImages();
                    });
                });

            } catch (Exception e) {
                Log.e(TAG, "Error during prediction", e);
                requireActivity().runOnUiThread(() -> {
                    showCustomToast("AI prediction failed. Uploading without prediction...");
                    uploadMultipleImages();
                });
            }
        }).start();
    }

    private int getSpinnerIndexByValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Ensemble prediction: combines multiple predictions to get best result
     * Strategy: Uses weighted voting based on confidence scores
     */
    private ClassificationResult ensemblePrediction(List<ClassificationResult> results) {
        if (results.isEmpty()) {
            return new ClassificationResult("Unknown", 0f);
        }

        Map<String, Float> votes = new HashMap<>();

        for (ClassificationResult result : results) {
            String label = result.label;
            float confidence = result.confidence;

            votes.put(label, votes.getOrDefault(label, 0f) + confidence);
        }

        String bestLabel = "Unknown";
        float bestScore = 0f;

        for (Map.Entry<String, Float> entry : votes.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestLabel = entry.getKey();
            }
        }

        int count = 0;
        float totalConfidence = 0f;

        for (ClassificationResult result : results) {
            if (result.label.equals(bestLabel)) {
                totalConfidence += result.confidence;
                count++;
            }
        }

        float avgConfidence = count > 0 ? totalConfidence / count : 0f;

        Log.d(TAG, "Ensemble voting results:");
        for (Map.Entry<String, Float> entry : votes.entrySet()) {
            Log.d(TAG, "  " + entry.getKey() + ": " + entry.getValue());
        }

        return new ClassificationResult(bestLabel, avgConfidence);
    }

    /**
     * Update the image preview with horizontal scroll
     */
    private void updateImagePreview() {
        imagePreviewContainer.removeAllViews();
        imagePreviewScrollView.setVisibility(View.VISIBLE);

        for (int i = 0; i < imageUris.size(); i++) {
            final int index = i;
            Uri uri = imageUris.get(i);

            FrameLayout frameLayout = new FrameLayout(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    300,
                    300
            );
            params.setMargins(8, 8, 8, 8);
            frameLayout.setLayoutParams(params);

            ImageView imageView = new ImageView(requireContext());
            imageView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(uri).into(imageView);

            ImageButton removeButton = new ImageButton(requireContext());
            FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(60, 60);
            btnParams.gravity = Gravity.TOP | Gravity.END;
            btnParams.setMargins(8, 8, 8, 8);
            removeButton.setLayoutParams(btnParams);
            removeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            removeButton.setBackgroundColor(0xCC000000);
            removeButton.setOnClickListener(v -> {
                imageUris.remove(index);
                updateImagePreview();
                showCustomToast("Image removed. " + imageUris.size() + " remaining.");
            });

            frameLayout.addView(imageView);
            frameLayout.addView(removeButton);
            imagePreviewContainer.addView(frameLayout);
        }

        btnPickImage.setText(imageUris.size() + "/" + MAX_IMAGES + " images selected");

        if (imageUris.size() >= MIN_IMAGES) {
            btnPickImage.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            btnPickImage.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        }
    }

    private void uploadMultipleImages() {
        List<String> uploadedUrls = new ArrayList<>();
        AtomicInteger uploadCounter = new AtomicInteger(0);
        final int totalImages = imageUris.size();

        for (Uri imageUri : imageUris) {
            MediaManager.get().upload(imageUri)
                    .unsigned("mushrooms")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Upload started: " + requestId);
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String cloudinaryUrl = resultData.get("secure_url").toString();
                            uploadedUrls.add(cloudinaryUrl);

                            int completed = uploadCounter.incrementAndGet();
                            showCustomToast("Uploaded " + completed + "/" + totalImages);

                            if (completed == totalImages) {
                                savePostToFirestore(uploadedUrls);
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            showCustomToast("Upload failed: " + error.getDescription());
                            Log.e(TAG, "Upload error: " + error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();
        }
    }

    /**
     * Save post with multiple image URLs to Firestore
     */
    private void savePostToFirestore(List<String> imageUrl) {
        getCurrentUsername(username -> {
            String fullLocation = getAddressFromCoordinates(latitude, longitude);

            Map<String, Object> post = new HashMap<>();
            post.put("mushroomType", etMushroomType.getText().toString().trim());
            post.put("category", spinnerCategory.getSelectedItem().toString());
            post.put("description", etDescription.getText().toString().trim());
            post.put("latitude", latitude);
            post.put("longitude", longitude);
            post.put("location", fullLocation);
            post.put("imageUrl", imageUrl);
            post.put("timestamp", System.currentTimeMillis());
            post.put("userId", getCurrentUserId());
            post.put("username", username);

            String verifiedStatus;

            float confidencePercentage = detectedConfidence;
            if (confidencePercentage > 0 && confidencePercentage <= 1.0f) {
                confidencePercentage *= 100;
            }

            Log.d(TAG, "===== VERIFICATION CALCULATION =====");
            Log.d(TAG, "Original confidence: " + detectedConfidence);
            Log.d(TAG, "Confidence percentage: " + confidencePercentage);
            Log.d(TAG, "Detected class: '" + detectedClass + "'");

            if (detectedClass == null || detectedClass.trim().isEmpty() ||
                    "Unknown".equalsIgnoreCase(detectedClass.trim())) {
                verifiedStatus = "not verified";
                Log.d(TAG, "Status: not verified (Unknown class)");
            } else if (confidencePercentage >= 85.0f) {
                verifiedStatus = "verified";
                Log.d(TAG, "Status: verified (confidence >= 85%)");
            } else {
                verifiedStatus = "unreliable";
                Log.d(TAG, "Status: unreliable (confidence < 85%)");
            }

            Log.d(TAG, "Final verified status: " + verifiedStatus);
            Log.d(TAG, "===================================");

            post.put("verified", verifiedStatus);

            FirebaseFirestore.getInstance()
                    .collection("posts")
                    .add(post)
                    .addOnSuccessListener(documentReference -> {
                        documentReference.update("postId", documentReference.getId());

                        Log.d(TAG, "Post saved with ID: " + documentReference.getId());
                        Log.d(TAG, "Verified status in post: " + verifiedStatus);
                        Log.d(TAG, "Number of images: " + imageUrl.size());

                        showCustomToast("Post saved with " + imageUrl.size() + " images!");

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                requireActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.fragmentContainer, new HomeFragment())
                                        .commitAllowingStateLoss();
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving post", e);
                        showCustomToast("Error saving post: " + e.getMessage());
                    });
        });
    }

    private void showCustomToast(String message) {
        if (!isAdded()) return;

        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, null);

        TextView toastText = layout.findViewById(R.id.toast_text);
        toastText.setText(message);

        Toast toast = new Toast(requireContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        } else {
            showCustomToast("Location permission is required.");
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted!");
            showCustomToast("Location permission is required.");
            return;
        }

        showCustomToast("Loading... This May Take a While");

        com.google.android.gms.location.LocationRequest locationRequest =
                com.google.android.gms.location.LocationRequest.create()
                        .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(0)
                        .setFastestInterval(0)
                        .setNumUpdates(1);

        fusedLocationClient.requestLocationUpdates(locationRequest,
                new com.google.android.gms.location.LocationCallback() {
                    @Override
                    public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                        if (locationResult == null) {
                            Log.e(TAG, "Fresh location request returned null.");
                            showCustomToast("Unable to fetch location.");
                            return;
                        }

                        android.location.Location location = locationResult.getLastLocation();
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        String fullAddress = getAddressFromCoordinates(latitude, longitude);

                        String plusCode = null;
                        if (fullAddress != null && fullAddress.matches("^[0-9A-Z]{4}\\+[0-9A-Z]{2,}.*")) {
                            int commaIndex = fullAddress.indexOf(',');
                            plusCode = commaIndex > 0 ? fullAddress.substring(0, commaIndex) : fullAddress;
                        }

                        if (plusCode != null) {
                            userLocation = plusCode + " • " + fullAddress;
                        } else {
                            userLocation = fullAddress != null ? fullAddress : latitude + "," + longitude;
                        }

                        Log.d(TAG, "Fresh location fetched: " + userLocation);

                        tvLatitude.setText("Y: " + latitude);
                        tvLongitude.setText("X: " + longitude);

                        geoContainer.setVisibility(View.VISIBLE);
                        coordinates.setVisibility(View.VISIBLE);

                        setupMiniMap();

                        showCustomToast("Location captured!");
                    }
                },
                null);
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "anonymous";
    }

    private String getAddressFromCoordinates(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(requireContext());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                StringBuilder fullAddress = new StringBuilder();

                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0) fullAddress.append(", ");
                    fullAddress.append(address.getAddressLine(i));
                }

                String result = fullAddress.toString();

                if (result.isEmpty()) {
                    return lat + "," + lon;
                }

                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "Geocoder error: " + e.getMessage());
            e.printStackTrace();
        }

        return lat + "," + lon;
    }

    private void setupMiniMap() {
        try {
            miniMapView.getController().setZoom(15.0);

            if (latitude != 0.0 && longitude != 0.0) {
                GeoPoint userLocation = new GeoPoint(latitude, longitude);
                miniMapView.getController().setCenter(userLocation);

                miniMapView.getOverlays().clear();

                Uri firstImageUri = !imageUris.isEmpty() ? imageUris.get(0) : null;

                if (firstImageUri != null) {
                    Glide.with(this)
                            .asBitmap()
                            .load(firstImageUri)
                            .circleCrop()
                            .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull android.graphics.Bitmap resource,
                                                            @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                                    int size = 70;
                                    android.graphics.Bitmap smallBitmap = android.graphics.Bitmap.createScaledBitmap(resource, size, size, false);

                                    Marker marker = new Marker(miniMapView);
                                    marker.setPosition(userLocation);
                                    marker.setIcon(new android.graphics.drawable.BitmapDrawable(getResources(), smallBitmap));
                                    marker.setTitle("Your Location");
                                    miniMapView.getOverlays().add(marker);

                                    miniMapView.invalidate();
                                }

                                @Override
                                public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
                            });
                } else {
                    Marker marker = new Marker(miniMapView);
                    marker.setPosition(userLocation);
                    marker.setTitle("Your Location");
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    miniMapView.getOverlays().add(marker);
                }

                miniMapView.setOnClickListener(v -> openFullMap());
                btnOpenFullMap.setOnClickListener(v -> openFullMap());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void openFullMap() {
        Intent intent = new Intent(requireContext(), FullMap.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        if (!imageUris.isEmpty()) {
            intent.putExtra("imageUri", imageUris.get(0).toString());
        }
        startActivity(intent);
    }

    private void getCurrentUsername(OnUsernameFetchedListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            listener.onUsernameFetched(username != null ? username : "anonymous");
                        } else {
                            listener.onUsernameFetched("anonymous");
                        }
                    })
                    .addOnFailureListener(e -> listener.onUsernameFetched("anonymous"));
        } else {
            listener.onUsernameFetched("anonymous");
        }
    }

    interface OnUsernameFetchedListener {
        void onUsernameFetched(String username);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapPreview != null) {
            mapPreview.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapPreview != null) {
            mapPreview.onPause();
        }
    }

    private boolean containsBannedWord(String input) {
        if (input == null || input.isEmpty()) return false;
        String lower = input.toLowerCase();
        for (String word : bannedWords) {
            String regex = word.replaceAll("(.)", "$1+");
            if (lower.matches(".*" + regex + ".*")) {
                return true;
            }
        }
        return false;
    }

    private String censorBadWords(String input) {
        String result = input;
        for (String word : bannedWords) {
            String regex = word.replaceAll("(.)", "$1+");
            result = result.replaceAll("(?i)" + regex, "****");
        }
        return result;
    }

    private void loadBannedWords() {
        try {
            InputStream is = requireContext().getAssets().open("censored-words.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(json);

            String[] categories = {"profanity", "insults", "sexual", "violence", "drugs", "slurs"};
            for (String cat : categories) {
                if (obj.has(cat)) {
                    JSONArray arr = obj.getJSONArray(cat);
                    for (int i = 0; i < arr.length(); i++) {
                        bannedWords.add(arr.getString(i).toLowerCase());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showCustomToast("Failed to load banned words");
        }
    }

    private void fetchEdibility(String mushroomName, OnEdibilityFetchedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("mushroom-encyclopedia")
                .whereEqualTo("mushroomName", mushroomName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String simplified = "Unknown";

                    if (!queryDocumentSnapshots.isEmpty()) {
                        String edibility = queryDocumentSnapshots.getDocuments()
                                .get(0)
                                .getString("edibility");

                        if (edibility != null) {
                            switch (edibility.toLowerCase()) {
                                case "edible":
                                case "ediblew":
                                    simplified = "Edible";
                                    break;
                                case "inedible":
                                case "inediblemed":
                                    simplified = "Inedible";
                                    break;
                                case "poisonous":
                                    simplified = "Poisonous";
                                    break;
                            }
                        }
                    }

                    listener.onEdibilityFetched(simplified);

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch edibility", e);
                    showCustomToast("Error fetching edibility data");
                    listener.onEdibilityFetched("Unknown");
                });
    }

    interface OnEdibilityFetchedListener {
        void onEdibilityFetched(String simplifiedEdibility);
    }
}