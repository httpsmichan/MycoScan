package com.example.myapplication;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private List<Post> postList;
    private FirebaseFirestore db;
    private boolean isHistory;
    private OnItemClickListener listener;
    private OnStatusChangeListener statusChangeListener;

    public interface OnItemClickListener {
        void onItemClick(Post post);
    }

    public interface OnStatusChangeListener {
        void onStatusChanged();
    }

    public PostsAdapter(List<Post> postList, boolean isHistory, OnItemClickListener listener, OnStatusChangeListener statusChangeListener) {
        this.postList = postList;
        this.db = FirebaseFirestore.getInstance();
        this.isHistory = isHistory;
        this.listener = listener;
        this.statusChangeListener = statusChangeListener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isHistory ? R.layout.item_post_history : R.layout.post_item;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new PostViewHolder(view, isHistory);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        if (holder.imageVerifiedBadge != null) {
            holder.imageVerifiedBadge.setVisibility(View.GONE);
        }

        if (post.getUserId() != null && !post.getUserId().isEmpty() && holder.imageVerifiedBadge != null) {
            db.collection("users").document(post.getUserId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Object verifiedObj = documentSnapshot.get("verified");
                            boolean isVerified = false;

                            if (verifiedObj instanceof Boolean) {
                                isVerified = (Boolean) verifiedObj;
                            } else if (verifiedObj instanceof String) {
                                isVerified = "true".equalsIgnoreCase((String) verifiedObj);
                            }

                            Log.d("PostsAdapter", "User: " + post.getUsername() +
                                    " | UserID: " + post.getUserId() +
                                    " | Verified: " + isVerified);

                            if (isVerified) {
                                holder.imageVerifiedBadge.setVisibility(View.VISIBLE);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("PostsAdapter", "Error checking verification: " + e.getMessage());
                    });
        }

        if (holder.imageView != null) {
            String imageUrl = post.getFirstImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(android.R.drawable.ic_menu_report_image)
                        .error(android.R.drawable.ic_menu_close_clear_cancel)
                        .into(holder.imageView);
            } else {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }

        if (holder.textMushroomType != null)
            holder.textMushroomType.setText(post.getMushroomType() != null ? post.getMushroomType() : "Unknown");

        if (holder.textPostedBy != null)
            holder.textPostedBy.setText("Posted by: " + (post.getUsername() != null ? post.getUsername() : "Unknown"));

        if (holder.textDescription != null)
            holder.textDescription.setText(post.getDescription() != null ? post.getDescription() : "");

        if (holder.textLocation != null)
            holder.textLocation.setText(post.getLocation() != null ? post.getLocation() : "Unknown");

        if (isHistory && holder.textVerified != null) {
            String status = post.getVerified();
            if (status != null) {
                holder.textVerified.setText(status.equalsIgnoreCase("unreliable") ? "Unreliable" : status);
                holder.textVerified.setBackgroundColor(
                        status.equalsIgnoreCase("unreliable") ? Color.RED : Color.parseColor("#4CAF50"));
            } else holder.textVerified.setVisibility(View.GONE);
        }

        if (holder.textCategory != null) {
            String category = post.getCategory() != null ? post.getCategory() : "Unknown";
            holder.textCategory.setText(category);
            int bgColor;
            switch (category.toLowerCase()) {
                case "edible": bgColor = 0xFF4CAF50; break;
                case "inedible": bgColor = 0xFF9E9E9E; break;
                case "poisonous": bgColor = 0xFFF44336; break;
                case "medicinal": bgColor = 0xFF2196F3; break;
                default: bgColor = 0xFF9E9E9E; break;
            }
            holder.textCategory.setBackgroundColor(bgColor);
        }

        if (!isHistory) {
            holder.identityLayout.setVisibility(View.GONE);
            holder.btnVerify.setVisibility(View.VISIBLE);
            holder.btnDecline.setVisibility(View.VISIBLE);

            if ("verified".equalsIgnoreCase(post.getVerified()) ||
                    "unreliable".equalsIgnoreCase(post.getVerified())) {
                holder.btnVerify.setVisibility(View.GONE);
                holder.btnDecline.setVisibility(View.GONE);
            }

            holder.btnVerify.setOnClickListener(v -> updatePostVerification(post.getPostId(), "verified", position));

            holder.btnDecline.setOnClickListener(v -> {
                holder.identityLayout.setVisibility(View.VISIBLE);
                holder.btnVerify.setVisibility(View.GONE);
                holder.btnDecline.setVisibility(View.GONE);
            });

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    holder.itemView.getContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"Edible", "Poisonous", "Inedible (Non-toxic)", "Medicinal"}
            );
            holder.spinnerCorrectedCategory.setAdapter(spinnerAdapter);

            holder.btnDone.setOnClickListener(doneView -> {
                String correctedName = holder.etCorrectedName.getText().toString().trim();
                String correctedCategory = holder.spinnerCorrectedCategory.getSelectedItem().toString();

                if (correctedName.isEmpty()) {
                    holder.etCorrectedName.setError("Please enter corrected name");
                    return;
                }

                String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : null;

                if (currentUid == null) {
                    Toast.makeText(holder.itemView.getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.collection("users").document(currentUid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists() && doc.contains("username")) {
                                String adminUsername = doc.getString("username");

                                Map<String, Object> updateData = new HashMap<>();
                                updateData.put("verified", "unreliable");
                                updateData.put("correction", correctedName);
                                updateData.put("correct_category", correctedCategory);
                                updateData.put("verified_by", adminUsername);

                                db.collection("posts").document(post.getPostId())
                                        .update(updateData)
                                        .addOnSuccessListener(aVoid -> {
                                            postList.remove(position);
                                            notifyItemRemoved(position);
                                            Toast.makeText(holder.itemView.getContext(),
                                                    "Post updated and moved to history", Toast.LENGTH_SHORT).show();
                                            holder.identityLayout.setVisibility(View.GONE);
                                            if (statusChangeListener != null) statusChangeListener.onStatusChanged();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(holder.itemView.getContext(),
                                                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(holder.itemView.getContext(),
                                        "Admin username not found in users collection", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(holder.itemView.getContext(),
                                        "Error fetching username: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(post);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    private void updatePostVerification(String postId, String status, int position) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUid == null) return;

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    String adminUsername = doc.getString("username");
                    Map<String, Object> update = new HashMap<>();
                    update.put("verified", status);
                    update.put("verified_by", adminUsername);

                    db.collection("posts").document(postId)
                            .update(update)
                            .addOnSuccessListener(aVoid -> {
                                postList.remove(position);
                                notifyItemRemoved(position);
                                if (statusChangeListener != null) statusChangeListener.onStatusChanged();
                            });
                });
    }

    public void updateList(List<Post> newList) {
        this.postList.clear();
        this.postList.addAll(newList);
        notifyDataSetChanged();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textMushroomType, textPostedBy, textDescription, textVerified, textLocation, textCategory;
        Button btnVerify, btnDecline;
        LinearLayout identityLayout;
        TextView btnDone;
        EditText etCorrectedName;
        Spinner spinnerCorrectedCategory;
        ImageView imageVerifiedBadge;


        public PostViewHolder(@NonNull View itemView, boolean isHistory) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViews);
            textMushroomType = itemView.findViewById(R.id.textMushroomType);
            textPostedBy = itemView.findViewById(R.id.textPostedBy);
            textDescription = itemView.findViewById(R.id.textDescription);
            textLocation = itemView.findViewById(R.id.textLocation);

            imageVerifiedBadge = itemView.findViewById(R.id.imageVerifiedBadge);

            if (!isHistory) {
                btnVerify = itemView.findViewById(R.id.btnVerify);
                btnDecline = itemView.findViewById(R.id.btnDecline);
                identityLayout = itemView.findViewById(R.id.identity);
                btnDone = itemView.findViewById(R.id.btnDone);
                etCorrectedName = itemView.findViewById(R.id.etCorrectedName);
                spinnerCorrectedCategory = itemView.findViewById(R.id.spinnerCorrectedCategory);
            }

            if (isHistory) {
                textVerified = itemView.findViewById(R.id.textVerified);
            }
        }
    }
}