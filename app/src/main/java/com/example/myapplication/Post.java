package com.example.myapplication;

public class Post {
    private String postId;
    private String mushroomType;
    private String description;
    private String imageUrl;
    private String userId;
    private String username;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String verified;
    private String location;
    private String category;



    public Post() {}

    public Post(String mushroomType, String description, String imageUrl,
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
    public String getImageUrl() { return imageUrl; }
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
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setVerified(String verified) { this.verified = verified; }
    public void setLocation(String location) { this.location = location; }
    public void setCategory(String category) { this.category = category; }
}