package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;

public class Post {
    private String postId;
    private String mushroomType;
    private String description;
    private Object imageUrl;
    private String userId;
    private String username;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String verified;
    private String location;
    private String category;

    public Post() {}

    public Post(String mushroomType, String description, Object imageUrl,
                String userId, String username, double latitude, double longitude,
                long timestamp, String verified, String location, String category) {
        this.mushroomType = mushroomType;
        this.description = description;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.verified = verified;
        this.location = location;
        this.category = category;
    }

    public String getPostId() { return postId; }
    public String getMushroomType() { return mushroomType; }
    public String getDescription() { return description; }
    public Object getImageUrl() { return imageUrl; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getTimestamp() { return timestamp; }
    public String getVerified() { return verified; }
    public String getLocation() { return location; }
    public String getCategory() { return category; }

    public void setPostId(String postId) { this.postId = postId; }
    public void setMushroomType(String mushroomType) { this.mushroomType = mushroomType; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(Object imageUrl) { this.imageUrl = imageUrl; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setVerified(String verified) { this.verified = verified; }
    public void setLocation(String location) { this.location = location; }
    public void setCategory(String category) { this.category = category; }

    public String getFirstImageUrl() {
        if (imageUrl instanceof String) {
            return (String) imageUrl;
        } else if (imageUrl instanceof List) {
            List<?> urls = (List<?>) imageUrl;
            if (!urls.isEmpty()) {
                return urls.get(0).toString(); // Get FIRST image
            }
        }
        return null;
    }

    public List<String> getAllImageUrls() {
        List<String> urls = new ArrayList<>();
        if (imageUrl instanceof String) {
            urls.add((String) imageUrl);
        } else if (imageUrl instanceof List) {
            for (Object obj : (List<?>) imageUrl) {
                if (obj instanceof String) urls.add((String) obj);
            }
        }
        return urls;
    }

    public int getImageCount() {
        if (imageUrl == null) {
            return 0;
        }

        if (imageUrl instanceof String) {
            return 1;
        } else if (imageUrl instanceof List) {
            return ((List<?>) imageUrl).size();
        }

        return 0;
    }
}