package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MushroomAdapter extends RecyclerView.Adapter<MushroomAdapter.ViewHolder> {

    private List<String> mushrooms;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String mushroomName);
    }

    public MushroomAdapter(List<String> mushrooms, OnItemClickListener listener) {
        this.mushrooms = mushrooms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mushroom, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = mushrooms.get(position);
        holder.textView.setText(name);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(name);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mushrooms.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.mushroomName);
        }
    }
}
