package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(String userId);
    }

    private List<User> users;
    private OnUserClickListener listener;

    public UserAdapter(List<User> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_result, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.username.setText(user.getUsername());

        if (user.getProfilePhoto() != null && !user.getProfilePhoto().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfilePhoto())
                    .circleCrop()
                    .into(holder.profileImage);
        }

        holder.itemView.setOnClickListener(v -> listener.onUserClick(user.getUserId()));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        ImageView profileImage;

        UserViewHolder(View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.usernameText);
            profileImage = itemView.findViewById(R.id.profileImage);
        }
    }
}

