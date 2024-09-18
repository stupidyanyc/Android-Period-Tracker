package com.example.fyp20;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Post {
    private String id;
    private String content;
    private String photoUrl;
    private String videoUrl;
    private Map<String, Boolean> likes;
    private boolean isBookmarked;
    private long timestamp;

    public Post() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public Post(String id, String content, String photoUrl, String videoUrl) {
        this.id = id;
        this.content = content;
        this.photoUrl = photoUrl;
        this.videoUrl = videoUrl;
        this.likes = new HashMap<>();
        this.isBookmarked = false;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Boolean> getLikes() {
        return likes;
    }

    public void setLikes(Map<String, Boolean> likes) {
        this.likes = likes;
    }

    public int getLikeCount() {
        return likes != null ? likes.size() : 0;
    }

    public boolean isLikedBy(String userId) {
        return likes != null && likes.containsKey(userId);
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        isBookmarked = bookmarked;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean hasMedia() {
        return (photoUrl != null && !photoUrl.isEmpty()) || (videoUrl != null && !videoUrl.isEmpty());
    }
}