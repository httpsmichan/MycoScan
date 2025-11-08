package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class UserPostAdapter extends RecyclerView.Adapter<UserPostAdapter.UserPostViewHolder> {

    private Context context;
    private List<Post> postList;

    public UserPostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public UserPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_post_item, parent, false);
        return new UserPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserPostViewHolder holder, int position) {
        Post post = postList.get(position);

        // Use getFirstImageUrl() instead of getImageUrl()
        String imageUrl = post.getFirstImageUrl();

        Glide.with(context)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.imagePost);

        holder.imagePost.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("postId", post.getPostId());
            intent.putExtra("imageUrl", post.getFirstImageUrl()); // Use getFirstImageUrl()
            intent.putExtra("mushroomType", post.getMushroomType());
            intent.putExtra("description", post.getDescription());
            intent.putExtra("username", post.getUsername());
            intent.putExtra("latitude", post.getLatitude());
            intent.putExtra("longitude", post.getLongitude());
            intent.putExtra("verified", post.getVerified());
            intent.putExtra("location", post.getLocation());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class UserPostViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePost;

        public UserPostViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePost = itemView.findViewById(R.id.imagePost);
        }
    }
}