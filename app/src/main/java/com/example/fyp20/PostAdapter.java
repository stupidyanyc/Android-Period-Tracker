package com.example.fyp20;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private List<Post> posts;
    private DatabaseReference postsRef;
    private String currentUserId;

    public PostAdapter(List<Post> posts, DatabaseReference postsRef, String currentUserId) {
        this.posts = posts;
        this.postsRef = postsRef;
        this.currentUserId = currentUserId;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        TextView contentTextView;
        ImageView postImageView;
        VideoView postVideoView;
        MaterialButton likeButton, commentButton, shareButton, bookmarkButton;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            postImageView = itemView.findViewById(R.id.postImageView);
            postVideoView = itemView.findViewById(R.id.postVideoView);
            likeButton = itemView.findViewById(R.id.likeButton);
            commentButton = itemView.findViewById(R.id.commentButton);
            shareButton = itemView.findViewById(R.id.shareButton);
            bookmarkButton = itemView.findViewById(R.id.bookmarkButton);
        }

        void bind(Post post) {
            contentTextView.setText(post.getContent());
            updateLikeButton(post);
            bookmarkButton.setIconTint(ColorStateList.valueOf(post.isBookmarked() ?
                    ContextCompat.getColor(itemView.getContext(), R.color.primary_color) :
                    ContextCompat.getColor(itemView.getContext(), R.color.secondary_text_color)));

            if (post.getPhotoUrl() != null && !post.getPhotoUrl().isEmpty()) {
                postImageView.setVisibility(View.VISIBLE);
                postVideoView.setVisibility(View.GONE);
                Glide.with(itemView.getContext()).load(post.getPhotoUrl()).into(postImageView);
            } else if (post.getVideoUrl() != null && !post.getVideoUrl().isEmpty()) {
                postImageView.setVisibility(View.GONE);
                postVideoView.setVisibility(View.VISIBLE);
                postVideoView.setVideoPath(post.getVideoUrl());
                postVideoView.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    postVideoView.start();
                });
            } else {
                postImageView.setVisibility(View.GONE);
                postVideoView.setVisibility(View.GONE);
            }

            likeButton.setOnClickListener(v -> toggleLike(post));

            commentButton.setOnClickListener(v -> {
                Toast.makeText(itemView.getContext(), "Comment clicked", Toast.LENGTH_SHORT).show();
            });

            shareButton.setOnClickListener(v -> {
                Toast.makeText(itemView.getContext(), "Share clicked", Toast.LENGTH_SHORT).show();
            });

            bookmarkButton.setOnClickListener(v -> {
                post.setBookmarked(!post.isBookmarked());
                postsRef.child(post.getId()).child("bookmarked").setValue(post.isBookmarked());
            });
        }

        private void updateLikeButton(Post post) {
            boolean isLiked = post.isLikedBy(currentUserId);
            likeButton.setText(String.format("Like (%d)", post.getLikeCount()));
            likeButton.setIconTint(ColorStateList.valueOf(isLiked ?
                    ContextCompat.getColor(itemView.getContext(), R.color.primary_color) :
                    ContextCompat.getColor(itemView.getContext(), R.color.secondary_text_color)));
        }

        private void toggleLike(Post post) {
            DatabaseReference likesRef = postsRef.child(post.getId()).child("likes");
            if (post.isLikedBy(currentUserId)) {
                likesRef.child(currentUserId).removeValue();
            } else {
                likesRef.child(currentUserId).setValue(true);
            }
        }
    }
}