package com.example.fyp20;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ForumActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_VIDEO_REQUEST = 2;

    private TextInputEditText postEditText;
    private MaterialButton postButton, addPhotoButton, addVideoButton;
    private ImageView previewImageView;
    private VideoView previewVideoView;
    private TextInputEditText searchEditText;
    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
    private List<Post> posts;
    private BottomNavigationView bottomNavigationView;
    private DatabaseReference postsRef;
    private StorageReference storageRef;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private Uri selectedMediaUri;
    private boolean isPhotoSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        postsRef = database.getReference("posts");
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        postEditText = findViewById(R.id.postEditText);
        postButton = findViewById(R.id.postButton);
        addPhotoButton = findViewById(R.id.addPhotoButton);
        addVideoButton = findViewById(R.id.addVideoButton);
        previewImageView = findViewById(R.id.previewImageView);
        previewVideoView = findViewById(R.id.previewVideoView);
        searchEditText = findViewById(R.id.searchEditText);
        postsRecyclerView = findViewById(R.id.postsRecyclerView);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        posts = new ArrayList<>();
        postAdapter = new PostAdapter(posts, postsRef, currentUserId);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsRecyclerView.setAdapter(postAdapter);

        postButton.setOnClickListener(v -> createPost());
        addPhotoButton.setOnClickListener(v -> openImageChooser());
        addVideoButton.setOnClickListener(v -> openVideoChooser());
        setupSearch();
        setupBottomNavigation();
        setupPostsListener();
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    private void openVideoChooser() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), PICK_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedMediaUri = data.getData();
            if (requestCode == PICK_IMAGE_REQUEST) {
                previewImageView.setImageURI(selectedMediaUri);
                previewImageView.setVisibility(View.VISIBLE);
                previewVideoView.setVisibility(View.GONE);
                isPhotoSelected = true;
            } else if (requestCode == PICK_VIDEO_REQUEST) {
                previewVideoView.setVideoURI(selectedMediaUri);
                previewVideoView.setVisibility(View.VISIBLE);
                previewImageView.setVisibility(View.GONE);
                isPhotoSelected = false;
            }
        }
    }

    private void setupPostsListener() {
        postsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                Post post = dataSnapshot.getValue(Post.class);
                if (post != null) {
                    posts.add(0, post);
                    postAdapter.notifyItemInserted(0);
                    postsRecyclerView.scrollToPosition(0);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                Post updatedPost = dataSnapshot.getValue(Post.class);
                if (updatedPost != null) {
                    for (int i = 0; i < posts.size(); i++) {
                        if (posts.get(i).getId().equals(updatedPost.getId())) {
                            posts.set(i, updatedPost);
                            postAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Post removedPost = dataSnapshot.getValue(Post.class);
                if (removedPost != null) {
                    for (int i = 0; i < posts.size(); i++) {
                        if (posts.get(i).getId().equals(removedPost.getId())) {
                            posts.remove(i);
                            postAdapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                // Handle moved child if necessary
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ForumActivity.this, "Failed to load posts.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createPost() {
        String content = postEditText.getText().toString().trim();
        if (!content.isEmpty() || selectedMediaUri != null) {
            String postId = postsRef.push().getKey();
            if (postId != null) {
                if (selectedMediaUri != null) {
                    uploadMediaAndCreatePost(postId, content);
                } else {
                    savePost(postId, content, null);
                }
            }
        }
    }

    private void uploadMediaAndCreatePost(String postId, String content) {
        String mediaPath = isPhotoSelected ? "images/" : "videos/";
        StorageReference mediaRef = storageRef.child(mediaPath + UUID.randomUUID().toString());

        mediaRef.putFile(selectedMediaUri)
                .addOnSuccessListener(taskSnapshot -> mediaRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> savePost(postId, content, uri.toString())))
                .addOnFailureListener(e -> Toast.makeText(ForumActivity.this, "Failed to upload media", Toast.LENGTH_SHORT).show());
    }

    private void savePost(String postId, String content, String mediaUrl) {
        Post newPost = new Post(postId, content, isPhotoSelected ? mediaUrl : null, isPhotoSelected ? null : mediaUrl);
        postsRef.child(postId).setValue(newPost)
                .addOnSuccessListener(aVoid -> {
                    postEditText.setText("");
                    previewImageView.setVisibility(View.GONE);
                    previewVideoView.setVisibility(View.GONE);
                    selectedMediaUri = null;
                    Toast.makeText(ForumActivity.this, "Post created", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(ForumActivity.this, "Failed to create post", Toast.LENGTH_SHORT).show());
    }


    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterPosts(s.toString());
            }
        });
    }

    private void filterPosts(String query) {
        List<Post> filteredPosts = new ArrayList<>();
        for (Post post : posts) {
            if (post.getContent().toLowerCase().contains(query.toLowerCase())) {
                filteredPosts.add(post);
            }
        }
        postAdapter.setPosts(filteredPosts);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        navigateToCalendar();
                        return true;
                    case R.id.navigation_forum:
                        return true;
                    case R.id.navigation_profile:
                        navigateToUserProfile();
                        return true;
                    case R.id.navigation_settings:
                        navigateToSettings();
                        return true;
                }
                return false;
            }
        });
        bottomNavigationView.setSelectedItemId(R.id.navigation_forum);
    }

    private void navigateToCalendar() {
        Intent intent = new Intent(this, CalendarActivity.class);
        startActivity(intent);
    }

    private void navigateToSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void navigateToUserProfile() {
        Intent intent = new Intent(this, UserProfileActivity.class);
        startActivity(intent);
    }
}