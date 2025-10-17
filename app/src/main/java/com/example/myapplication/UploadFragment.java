package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.api.IMapController;

public class UploadFragment extends Fragment {

    private EditText etMushroomType, etDescription;
    private Spinner spinnerCategory;
    private TextView btnPickImage, btnGetLocation;
    private AppCompatButton btnSubmit;
    private Uri imageUri;
    private String userLocation = "Unknown";

    private FusedLocationProviderClient fusedLocationClient;

    private static final int REQUEST_LOCATION = 101;
    private ImageView imagePreview;
    private TextView tvLatitude, tvLongitude;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private MapView mapPreview;
    private FrameLayout geoContainer;
    private LinearLayout coordinates;
    private MapView miniMapView;
    private TextView btnOpenFullMap;

    private List<String> bannedWords = new ArrayList<>();

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    if (imageUri != null) {
                        imagePreview.setImageURI(imageUri);
                        imagePreview.setVisibility(View.VISIBLE);
                        showCustomToast("Image selected!");
                    }
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
        imagePreview = root.findViewById(R.id.imagePreview);
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

        loadBannedWords();

        String[] categories = {"Edible", "Poisonous", "Inedible (Non-toxic)", "Medicinal", "Unknown / Needs ID"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
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

        btnSubmit.setOnClickListener(v -> {
            String mushroomType = etMushroomType.getText().toString().trim();
            if (mushroomType.isEmpty()) {
                mushroomType = "Unknown";
                etMushroomType.setText("Unknown");
            }

            String category = spinnerCategory.getSelectedItem().toString();
            String description = etDescription.getText().toString().trim();

            final String finalMushroomType = mushroomType;
            final String finalDescription = description;

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

            if (imageUri == null) {
                showCustomToast("Please select an image first.");
                return;
            }

            showCustomToast("Loading...");

            MediaManager.get().upload(imageUri)
                    .unsigned("mushrooms")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            showCustomToast("Uploading...");
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {

                            String cloudinaryUrl = resultData.get("secure_url").toString();

                            getCurrentUsername(username -> {
                                String fullLocation = getAddressFromCoordinates(latitude, longitude);

                                Map<String, Object> post = new HashMap<>();
                                post.put("mushroomType", etMushroomType.getText().toString().trim());
                                post.put("category", spinnerCategory.getSelectedItem().toString());
                                post.put("description", etDescription.getText().toString().trim());
                                post.put("latitude", latitude);
                                post.put("longitude", longitude);
                                post.put("location", fullLocation);
                                post.put("imageUrl", cloudinaryUrl);
                                post.put("timestamp", System.currentTimeMillis());
                                post.put("userId", getCurrentUserId());
                                post.put("username", username);
                                post.put("verified", "not verified");

                                FirebaseFirestore.getInstance()
                                        .collection("posts")
                                        .add(post)
                                        .addOnSuccessListener(documentReference -> {
                                            documentReference.update("postId", documentReference.getId());

                                            showCustomToast("Post saved!");

                                            if (isAdded()) {
                                                requireActivity().runOnUiThread(() -> {
                                                    requireActivity().getSupportFragmentManager()
                                                            .beginTransaction()
                                                            .replace(R.id.fragmentContainer, new HomeFragment())
                                                            .commitAllowingStateLoss();
                                                });
                                            }
                                        })
                                        .addOnFailureListener(e -> showCustomToast("Error saving post: " + e.getMessage()));
                            });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            showCustomToast("Upload failed: " + error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();
        });

        if (getArguments() != null) {
            String mushroomType = getArguments().getString("mushroomType");
            String category = getArguments().getString("category");
            String description = getArguments().getString("description");
            String photoUriString = getArguments().getString("photoUri");
            double lat = getArguments().getDouble("latitude", 0.0);
            double lon = getArguments().getDouble("longitude", 0.0);

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
                imageUri = Uri.parse(photoUriString);
                imagePreview.setImageURI(imageUri);
                imagePreview.setVisibility(View.VISIBLE);
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

        return root;
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, getView().findViewById(R.id.toast_root));

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
            Log.e("UploadFragment", "Location permissions not granted!");
            showCustomToast("Location permission is required.");
            return;
        }

        showCustomToast("Loading...");

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
                            Log.e("UploadFragment", "Fresh location request returned null.");
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

                        Log.d("UploadFragment", "Fresh location fetched: " + userLocation);

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
            Log.e("UploadFragment", "Geocoder error: " + e.getMessage());
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

                String imageUrl = (imageUri != null) ? imageUri.toString() : null;

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(this)
                            .asBitmap()
                            .load(imageUrl)
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
        if (imageUri != null) {
            intent.putExtra("imageUri", imageUri.toString());
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
}