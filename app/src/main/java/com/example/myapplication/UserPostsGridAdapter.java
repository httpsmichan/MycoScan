package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.SquareImageView;

import java.util.List;

public class UserPostsGridAdapter extends RecyclerView.Adapter<UserPostsGridAdapter.GridViewHolder> {

    private Context context;
    private List<Post> postList;

    public UserPostsGridAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_post_item, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        Post post = postList.get(position);

        // Use getFirstImageUrl() instead of getImageUrl()
        String imageUrl = post.getFirstImageUrl();
        Log.d("UserPostsGrid", "Loading image for post " + post.getPostId() + " with URL: " + imageUrl);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.imagePost.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .centerCrop()
                    .into(holder.imagePost);
        } else {
            Log.d("UserPostsGrid", "Image URL is null or empty, using placeholder");
            holder.imagePost.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        holder.imagePost.setOnClickListener(v -> {
            Log.d("UserPostsGrid", "Clicking post with ID: " + post.getPostId());

            Intent intent = new Intent(v.getContext(), PostDetailActivity.class);
            intent.putExtra("postId", post.getPostId());
            intent.putExtra("imageUrl", post.getFirstImageUrl()); // Use getFirstImageUrl()
            intent.putExtra("mushroomType", post.getMushroomType());
            intent.putExtra("description", post.getDescription());
            intent.putExtra("userId", post.getUserId());
            intent.putExtra("username", post.getUsername());
            intent.putExtra("latitude", post.getLatitude());
            intent.putExtra("longitude", post.getLongitude());
            intent.putExtra("verified", post.getVerified());

            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class GridViewHolder extends RecyclerView.ViewHolder {
        SquareImageView imagePost;

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePost = itemView.findViewById(R.id.imagePost);
        }
    }
}