package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.tvDetailUser.setText(post.getUsername() != null ? post.getUsername() : "Unknown");

        if (post.getUserId() != null && !post.getUserId().isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(post.getUserId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && Boolean.TRUE.equals(doc.getBoolean("verified"))) {
                            holder.imageVerifiedBadge.setVisibility(View.VISIBLE);
                        } else {
                            holder.imageVerifiedBadge.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> holder.imageVerifiedBadge.setVisibility(View.GONE));
        } else {
            holder.imageVerifiedBadge.setVisibility(View.GONE);
        }

        if (post.getTimestamp() > 0) {
            long now = System.currentTimeMillis();
            long diff = now - post.getTimestamp();

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            String timeText;
            if (hours < 24) {
                if (hours == 0) {
                    timeText = (minutes <= 1) ? "1 minute ago" : minutes + " minutes ago";
                } else {
                    timeText = hours + (hours == 1 ? " hour ago" : " hours ago");
                }
            } else if (days <= 5) {
                timeText = days + (days == 1 ? " day ago" : " days ago");
            } else {
                timeText = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(new Date(post.getTimestamp()));
            }

            holder.tvTimeStamp.setText(timeText);
        } else {
            holder.tvTimeStamp.setText("Unknown time");
        }

        String verified = post.getVerified() != null ? post.getVerified() : "Not Verified";

        if (verified.equalsIgnoreCase("verified")) {
            holder.ivVerificationBadge.setImageResource(R.drawable.ic_trusted_badge);
            holder.ivVerificationBadge.setVisibility(View.VISIBLE);
            holder.tvVerifiedStatus.setVisibility(View.GONE);
        } else if (verified.equalsIgnoreCase("Unreliable")) {
            holder.ivVerificationBadge.setImageResource(R.drawable.ic_not_trusted_badge);
            holder.ivVerificationBadge.setVisibility(View.VISIBLE);
            holder.tvVerifiedStatus.setVisibility(View.GONE);
        } else {
            holder.ivVerificationBadge.setVisibility(View.GONE);
            holder.tvVerifiedStatus.setText("Pending Review");
            holder.tvVerifiedStatus.setTextColor(holder.itemView.getResources().getColor(android.R.color.darker_gray));
            holder.tvVerifiedStatus.setVisibility(View.VISIBLE);
        }

        holder.tvMushroomType.setText(post.getMushroomType() != null ? post.getMushroomType() : "Unknown type");

        // Debug logging to see what we're getting
        Object rawImageData = post.getImageUrl();
        Log.d("PostAdapter", "Post ID: " + post.getPostId());
        Log.d("PostAdapter", "Image data type: " + (rawImageData != null ? rawImageData.getClass().getName() : "null"));
        Log.d("PostAdapter", "Image data value: " + rawImageData);

        // Get the first image URL (works for both String and List)
        String displayImageUrl = post.getFirstImageUrl();
        Log.d("PostAdapter", "Display URL: " + displayImageUrl);

        if (displayImageUrl != null && !displayImageUrl.isEmpty()) {
            Glide.with(holder.ivPostImage.getContext())
                    .load(displayImageUrl)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        if (post.getLocation() != null && !post.getLocation().isEmpty()) {
            holder.tvLocation.setText(post.getLocation());
            holder.ivLocationIcon.setVisibility(View.VISIBLE);
        } else {
            holder.tvLocation.setText("Unknown location");
            holder.ivLocationIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Log.d("PostAdapter", "Clicking post with ID: " + post.getPostId());

            Intent intent = new Intent(v.getContext(), PostDetailActivity.class);
            intent.putExtra("postId", post.getPostId());
            intent.putExtra("imageUrl", post.getFirstImageUrl());
            intent.putExtra("mushroomType", post.getMushroomType());
            intent.putExtra("userId", post.getUserId());
            intent.putExtra("username", post.getUsername());
            intent.putExtra("latitude", post.getLatitude());
            intent.putExtra("longitude", post.getLongitude());
            intent.putExtra("verified", post.getVerified());

            v.getContext().startActivity(intent);
        });

        holder.menuOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.menuOptions);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null && post.getUserId().equals(currentUser.getUid())) {
                popup.getMenu().add("Delete");
            } else {
                popup.getMenu().add("Report");
            }

            popup.setOnMenuItemClickListener(item -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;

                if (item.getTitle().equals("Delete")) {
                    deletePost(post.getPostId(), pos);
                } else if (item.getTitle().equals("Report")) {
                    reportPost(post.getPostId());
                }
                return true;
            });

            popup.show();
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("posts")
                    .document(post.getPostId())
                    .collection("votes")
                    .document(userId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (snapshot != null && snapshot.exists()) {
                            String type = snapshot.getString("type");
                            if ("upvote".equals(type)) {
                                holder.ivUpIcon.setColorFilter(context.getResources().getColor(android.R.color.holo_green_dark));
                                holder.ivDownIcon.setColorFilter(null);
                            } else if ("downvote".equals(type)) {
                                holder.ivDownIcon.setColorFilter(context.getResources().getColor(android.R.color.holo_red_dark));
                                holder.ivUpIcon.setColorFilter(null);
                            }
                        } else {
                            holder.ivUpIcon.setColorFilter(null);
                            holder.ivDownIcon.setColorFilter(null);
                        }
                    });

            holder.ivUpIcon.setOnClickListener(v -> {
                db.collection("posts")
                        .document(post.getPostId())
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

            holder.ivDownIcon.setOnClickListener(v -> {
                db.collection("posts")
                        .document(post.getPostId())
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

        String category = post.getCategory() != null ? post.getCategory() : "Unknown";
        holder.tvEdibility.setText(category);

        int bgColor;
        switch (category.toLowerCase()) {
            case "edible":
                bgColor = context.getResources().getColor(android.R.color.holo_green_light);
                break;
            case "inedible":
                bgColor = context.getResources().getColor(android.R.color.darker_gray);
                break;
            case "poisonous":
                bgColor = context.getResources().getColor(android.R.color.holo_red_light);
                break;
            case "medicinal":
                bgColor = context.getResources().getColor(android.R.color.holo_blue_light);
                break;
            default:
                bgColor = context.getResources().getColor(android.R.color.darker_gray);
                break;
        }

        float radius = 5 * context.getResources().getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(radius);

        holder.tvEdibility.setBackground(bg);
        holder.tvEdibility.setTextColor(context.getResources().getColor(android.R.color.white));
    }

    @Override
    public int getItemCount() {
        return postList.size();
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

    private void deletePost(String postId, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts").document(postId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    postList.remove(position);
                    notifyItemRemoved(position);
                    Log.d("PostAdapter", "Post deleted: " + postId);
                })
                .addOnFailureListener(e -> Log.e("PostAdapter", "Error deleting post", e));
    }

    private void reportPost(String postId) {
        Intent intent = new Intent(context, ReportActivity.class);
        intent.putExtra("postId", postId);
        context.startActivity(intent);
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvDetailUser, tvTimeStamp, tvMushroomType, tvVerifiedStatus, tvLocation, tvEdibility;
        ImageView ivPostImage, menuOptions, ivVerificationBadge, ivLocationIcon;
        ImageView ivUpIcon, ivDownIcon;
        ImageView imageVerifiedBadge;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDetailUser = itemView.findViewById(R.id.tvDetailUser);
            tvTimeStamp = itemView.findViewById(R.id.tvTimeStamp);
            tvVerifiedStatus = itemView.findViewById(R.id.tvVerifiedStatus);
            tvMushroomType = itemView.findViewById(R.id.tvMushroomType);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            menuOptions = itemView.findViewById(R.id.menuOptions);
            ivVerificationBadge = itemView.findViewById(R.id.ivVerificationBadge);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            ivLocationIcon = itemView.findViewById(R.id.ivLocationIcon);
            ivUpIcon = itemView.findViewById(R.id.ivUpIcon);
            ivDownIcon = itemView.findViewById(R.id.ivDownIcon);
            tvEdibility = itemView.findViewById(R.id.tvEdibility);
            imageVerifiedBadge = itemView.findViewById(R.id.imageVerifiedBadge);
        }
    }
}