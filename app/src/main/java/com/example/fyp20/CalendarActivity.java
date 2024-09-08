package com.example.fyp20;

import android.Manifest;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import android.net.Uri;

public class CalendarActivity extends AppCompatActivity {

    private static final String TAG = "CalendarActivity";

    private CompactCalendarView calendarView;
    private TextView textViewDate;
    private TextView textViewNextPeriod, textViewOvulation, textViewMiddlePain, textViewLutealPhase, textViewFollicularPhase;
    private MaterialButton btnStartPeriod, btnEndPeriod;
    private BottomNavigationView bottomNavigationView;
    private View dialogView;

    private Button btnEditNote;
    private Button btnDeleteNote;
    private TextView textViewNotes;
    private ImageView imageViewRecentNote;

    private Date selectedDate;
    private Date lastPeriodStartDate;
    private Date currentPeriodStartDate;
    private int averageCycleLength = 28;
    private int averagePeriodLength = 5;
    private String dateSelect;

    private SimpleDateFormat dateFormatForMonth = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
    private SimpleDateFormat dateFormatForDay = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static final int REQUEST_IMAGE_PICK = 2;
    private Uri photoUri;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        selectedDate = new Date();

        try {
            // Initialize Firebase Auth and Database references
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference("Users");

            initializeViews();
            setupCalendarView();
            setupButtons();
            setupBottomNavigation();

            // Load user-specific data like cycle, period, etc.
            loadUserData();

            // Fetch the current user from Firebase Authentication
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid(); // Get the user ID

                // Load the recent note for the selected date
                loadRecentNotes(userId, dateSelect);

                // Set up the "Add Note" button with the correct userId
                Button addNoteButton = findViewById(R.id.btnAddNote);
                addNoteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddNoteDialog(userId);  // Pass userId to the dialog
                    }
                });
            } else {
                Log.e(TAG, "User is not logged in.");
                Toast.makeText(this, "User not logged in. Please log in first.", Toast.LENGTH_SHORT).show();
                finish();  // Exit the activity if the user is not authenticated
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();  // Close the activity if initialization fails
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (lastPeriodStartDate != null) {
            updatePredictions(lastPeriodStartDate);
        }
        Log.d(TAG, "onResume called, calendar updated");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE && data != null) {
                // Capture image from camera
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                if (imageBitmap != null) {
                    // Set the new bitmap and update photoUri
                    photoUri = getImageUriFromBitmap(imageBitmap); // Method to convert bitmap to Uri
                    updateImageInDialog(imageBitmap);
                }
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                // Pick image from gallery
                photoUri = data.getData();
                if (photoUri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                        updateImageInDialog(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void updateImageInDialog(Bitmap imageBitmap) {
        ImageView imageView = dialogView.findViewById(R.id.imageViewPhotoEdit); // Assuming imageView is in dialog
        if (imageView != null) {
            imageView.setImageBitmap(imageBitmap);
            imageView.setVisibility(View.VISIBLE);
        }
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private void uploadPhotoAndSaveNote(Bitmap bitmap, String noteText) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("images")
                .child(userId)
                .child(UUID.randomUUID().toString() + ".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        storageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        // 在這裡，我們傳遞 null 作為 noteText，因為我們只更新圖片
                        // 現有的文本（如果有）將在 saveNoteWithImage 中被保留
                        saveNoteWithImage(userId, null, imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CalendarActivity.this, "Failed to upload photo", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error uploading photo", e);
                });
    }

    private void saveNoteWithImage(String userId, String noteText, String imageUrl) {
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Format the selected date to use it as the key in Firebase
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = dateFormat.format(selectedDate);

        // Reference the user's notes under the selected date
        DatabaseReference userNotesRef = mDatabase.child(userId).child("notes").child(formattedDate);

        checkExistingNote(userNotesRef, (existingNoteId, existingNoteData) -> {
            Map<String, Object> updatedNoteData = new HashMap<>();

            // Retain existing text and imageUrl if not updating them
            if (existingNoteData != null) {
                if (existingNoteData.containsKey("text")) {
                    updatedNoteData.put("text", existingNoteData.get("text"));
                }
                if (existingNoteData.containsKey("imageUrl")) {
                    updatedNoteData.put("imageUrl", existingNoteData.get("imageUrl"));
                }
            }

            // Update the note text or image URL (if new ones are provided)
            if (noteText != null && !noteText.trim().isEmpty()) {
                updatedNoteData.put("text", noteText);
            }
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                updatedNoteData.put("imageUrl", imageUrl);
            }

            updatedNoteData.put("timestamp", ServerValue.TIMESTAMP);

            String noteId = existingNoteId != null ? existingNoteId : userNotesRef.push().getKey();

            if (noteId != null) {
                userNotesRef.child(noteId).updateChildren(updatedNoteData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(CalendarActivity.this, "Note updated successfully", Toast.LENGTH_SHORT).show();
                            updateRecentNotes((String) updatedNoteData.get("text"), (String) updatedNoteData.get("imageUrl"));
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(CalendarActivity.this, "Failed to update note", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error updating note", e);
                        });
            }
        });
    }

    private void checkExistingNote(DatabaseReference userNotesRef, ExistingNoteCallback callback) {
        userNotesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    DataSnapshot firstChild = dataSnapshot.getChildren().iterator().next();
                    String noteId = firstChild.getKey();
                    Map<String, Object> noteData = (Map<String, Object>) firstChild.getValue();
                    callback.onResult(noteId, noteData);
                } else {
                    callback.onResult(null, null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error checking existing note: ", databaseError.toException());
                callback.onResult(null, null);
            }
        });
    }

    interface ExistingNoteCallback {
        void onResult(String noteId, Map<String, Object> noteData);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open the camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            } else {
                // Permission denied
                Toast.makeText(this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views");
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        calendarView = findViewById(R.id.compactcalendar_view);
        textViewDate = findViewById(R.id.textViewDate);
        textViewNextPeriod = findViewById(R.id.textViewNextPeriod);
        textViewOvulation = findViewById(R.id.textViewOvulation);
        textViewMiddlePain = findViewById(R.id.textViewMiddlePain);
        textViewLutealPhase = findViewById(R.id.textViewLutealPhase);
        textViewFollicularPhase = findViewById(R.id.textViewFollicularPhase);
        btnStartPeriod = findViewById(R.id.btnStartPeriod);
        btnEndPeriod = findViewById(R.id.btnEndPeriod);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnEditNote = findViewById(R.id.btnEditNote);
        btnDeleteNote = findViewById(R.id.btnDeleteNote);
        textViewNotes = findViewById(R.id.textViewNotes);
        imageViewRecentNote = findViewById(R.id.imageViewRecentNote);

        // Hide edit and delete button at initialization
        btnEditNote.setVisibility(View.GONE);
        btnDeleteNote.setVisibility(View.GONE);

        Log.d(TAG, "Views initialized successfully");
    }

    private void setupCalendarView() {
        Log.d(TAG, "Setting up calendar view");
        calendarView.setUseThreeLetterAbbreviation(true);
        calendarView.setFirstDayOfWeek(Calendar.MONDAY);

        calendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                selectedDate = dateClicked;
                textViewDate.setText(dateFormatForDay.format(dateClicked));
                dateSelect = new SimpleDateFormat("yyyy-MM-dd").format(dateClicked);
                textViewDate.setText(dateSelect);

                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    // Load the recent note for the selected date
                    loadRecentNotes(userId, dateSelect);
                }

                Log.d(TAG, "Selected date: " + dateFormatForDay.format(dateClicked));

            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                textViewDate.setText(dateFormatForMonth.format(firstDayOfNewMonth));
            }
        });

        calendarView.setCurrentDate(selectedDate);
        textViewDate.setText(dateFormatForMonth.format(selectedDate));
        dateSelect = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate);
        Log.d(TAG, "Calendar view set up successfully");
    }

    private void setupButtons() {
        Log.d(TAG, "Setting up buttons");
        btnStartPeriod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedDate != null) {
                    startPeriod(selectedDate);
                } else {
                    Toast.makeText(CalendarActivity.this, "Please select a date first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnEndPeriod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedDate != null) {
                    endPeriod(selectedDate);
                } else {
                    Toast.makeText(CalendarActivity.this, "Please select a date first", Toast.LENGTH_SHORT).show();
                }
            }
        });
        Log.d(TAG, "Buttons set up successfully");
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
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
    }

    private void navigateToSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void navigateToUserProfile() {
        Intent intent = new Intent(this, UserProfileActivity.class);
        startActivity(intent);
    }

    private void loadUserData() {
        Log.d(TAG, "Loading user data");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "Attempting to load data for user: " + userId);
            mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Log.d(TAG, "DataSnapshot exists. Raw data: " + dataSnapshot.getValue());
                        UserInfo userInfo = dataSnapshot.getValue(UserInfo.class);
                        if (userInfo != null) {
                            Log.d(TAG, "Successfully retrieved user data: " + userInfo.toString());
                            lastPeriodStartDate = new Date(userInfo.getLastPeriodYear() - 1900,
                                    userInfo.getLastPeriodMonth(),
                                    userInfo.getLastPeriodDay());

                            averagePeriodLength = parseIntegerFromString(userInfo.getPeriodLength(), 5);
                            averageCycleLength = parseIntegerFromString(userInfo.getCycleLength(), 28);

                            //根據新的update predictions, 這行將被移除
                            //updateCalendarWithPeriod(lastPeriodStartDate, 0);
                            updatePredictions(lastPeriodStartDate);
                            Log.d(TAG, "User data loaded and calendar updated");
                        } else {
                            Log.e(TAG, "Failed to convert DataSnapshot to UserInfo object");
                        }
                    } else {
                        Log.e(TAG, "DataSnapshot does not exist for user: " + userId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error loading user data: ", databaseError.toException());
                }
            });
        } else {
            Log.e(TAG, "Current user is null");
        }
    }

    private int parseIntegerFromString(String input, int defaultValue) {
        if (input == null || input.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(input.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing number from string: " + input, e);
            return defaultValue;
        }
    }

    private void loadRecentNotes(String userId, String selectedDate) {
        if (selectedDate != null) {
            DatabaseReference userNotesRef = mDatabase.child(userId).child("notes").child(selectedDate);

            userNotesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot noteSnapshot : dataSnapshot.getChildren()) {
                            String noteContent = noteSnapshot.child("text").getValue(String.class);
                            String imageUrl = noteSnapshot.child("imageUrl").getValue(String.class);
                            updateRecentNotes(noteContent, imageUrl);
                            showEditDeleteButtons(true);
                            break;  // 只顯示最近的一條筆記
                        }
                    } else {
                        updateRecentNotes(null, null);
                        showEditDeleteButtons(false);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error loading recent notes: ", databaseError.toException());
                }
            });
        }
    }

    private void showAddNoteDialog(String userId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        dialogView = getLayoutInflater().inflate(R.layout.dialog_add_note, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText noteEditText = dialogView.findViewById(R.id.editTextNote);
        Button addPhotoButton = dialogView.findViewById(R.id.btnAddPhoto);
        Button saveNoteButton = dialogView.findViewById(R.id.btnSaveNote);

        addPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPhotoOptions(); //show camera or gallery option
            }
        });

        saveNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String note = noteEditText.getText().toString();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    saveNoteToFirebase(userId, note, photoUri);
                    dialog.dismiss();
                } else {
                    Toast.makeText(CalendarActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void showPhotoOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Photo")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // Open camera
                                openCamera();
                                break;
                            case 1:
                                // Open gallery
                                openGallery();
                                break;
                        }
                    }
                })
                .show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted, start the camera
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            // Request the camera permission
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_CODE);
        }
    }

    private void openGallery() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
    }

    private void saveNoteToFirebase(String userId, String noteContent, Uri photoUri) {
        if (selectedDate == null) {
            Toast.makeText(CalendarActivity.this, "Please select a date first.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = dateFormat.format(selectedDate);

        DatabaseReference userNotesRef = mDatabase.child(userId).child("notes").child(formattedDate);

        checkExistingNote(userNotesRef, (existingNoteId, existingNoteData) -> {
            Map<String, Object> updatedNoteData = new HashMap<>();

            if (existingNoteData != null) {
                updatedNoteData.putAll(existingNoteData);
            }

            if (noteContent != null && !noteContent.trim().isEmpty()) {
                updatedNoteData.put("text", noteContent);
            }

            updatedNoteData.put("timestamp", ServerValue.TIMESTAMP);

            String noteId = existingNoteId != null ? existingNoteId : userNotesRef.push().getKey();

            if (photoUri != null) {
                uploadImageAndSaveNote(userNotesRef, noteId, updatedNoteData, photoUri);
            } else {
                saveNoteData(userNotesRef, noteId, updatedNoteData);
            }
        });
    }

    private void saveNoteData(DatabaseReference userNotesRef, String noteId, Map<String, Object> noteData) {
        userNotesRef.child(noteId).updateChildren(noteData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String noteContent = (String) noteData.get("text");
                        String imageUrl = (String) noteData.get("imageUrl");
                        updateRecentNotes(noteContent, imageUrl);
                        Toast.makeText(CalendarActivity.this, "Note saved successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CalendarActivity.this, "Failed to save note.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImageAndSaveNote(DatabaseReference userNotesRef, String noteId, Map<String, Object> noteData, Uri photoUri) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/" + UUID.randomUUID().toString());

        storageRef.putFile(photoUri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                noteData.put("imageUrl", uri.toString());
                saveNoteData(userNotesRef, noteId, noteData);
            }).addOnFailureListener(e -> {
                Toast.makeText(CalendarActivity.this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                saveNoteData(userNotesRef, noteId, noteData);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(CalendarActivity.this, "Failed to upload photo", Toast.LENGTH_SHORT).show();
            saveNoteData(userNotesRef, noteId, noteData);
        });
    }

    private void updateRecentNotes(String noteContent, String imageUrl) {
        TextView recentNotesTextView = findViewById(R.id.textViewNotes);
        ImageView recentNotesImageView = findViewById(R.id.imageViewRecentNote);

        boolean hasContent = false;

        if (noteContent != null && !noteContent.trim().isEmpty()) {
            recentNotesTextView.setVisibility(View.VISIBLE);
            recentNotesTextView.setText(noteContent);
            hasContent = true;
        } else {
            recentNotesTextView.setVisibility(View.GONE);
        }

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            recentNotesImageView.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(imageUrl)
                    .into(recentNotesImageView);
            hasContent = true;
        } else {
            recentNotesImageView.setVisibility(View.GONE);
        }

        if (!hasContent) {
            recentNotesTextView.setVisibility(View.VISIBLE);
            recentNotesTextView.setText("No notes for this date");
        }
    }

    private void showEditDeleteButtons(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        btnEditNote.setVisibility(visibility);
        btnDeleteNote.setVisibility(visibility);

        btnEditNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    String selectedDateStr = dateFormatForDay.format(selectedDate); // The selected date
                    DatabaseReference userNotesRef = mDatabase.child(userId).child("notes").child(selectedDateStr);

                    // Retrieve the note for the selected date
                    userNotesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (DataSnapshot noteSnapshot : dataSnapshot.getChildren()) {
                                    String noteId = noteSnapshot.getKey();
                                    String noteContent = noteSnapshot.child("text").getValue(String.class);
                                    String imageUrl = noteSnapshot.child("imageUrl").getValue(String.class);

                                    // Call the edit note dialog with the retrieved data
                                    showEditNoteDialog(userId, noteId, noteContent, imageUrl);
                                    break;  // Only handle the first note found for simplicity
                                }
                            } else {
                                Toast.makeText(CalendarActivity.this, "No note found to edit.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Error retrieving note for editing: ", databaseError.toException());
                        }
                    });
                } else {
                    Toast.makeText(CalendarActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
                }
            }
        });


        btnDeleteNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNote();
            }
        });
    }

    private void showEditNoteDialog(String userId, String noteId, String currentText, String currentImageUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_note, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText editTextNote = dialogView.findViewById(R.id.editTextNoteEdit);
        ImageView imageViewPhoto = dialogView.findViewById(R.id.imageViewPhotoEdit);
        Button btnRemovePhoto = dialogView.findViewById(R.id.btnRemovePhoto);
        Button btnReplacePhoto = dialogView.findViewById(R.id.btnReplacePhoto);
        Button btnSaveNote = dialogView.findViewById(R.id.btnSaveEditNote);
        Button btnCancelNote = dialogView.findViewById(R.id.btnCancelEditNote);

        // Set current note text
        editTextNote.setText(currentText);

        // Use a single-element array to hold the currentImageUrl, allowing it to be modified
        final String[] imageUrlHolder = {currentImageUrl};

        // Load and show current photo if available
        if (imageUrlHolder[0] != null && !imageUrlHolder[0].isEmpty()) {
            imageViewPhoto.setVisibility(View.VISIBLE);
            btnRemovePhoto.setVisibility(View.VISIBLE);
            Glide.with(this).load(imageUrlHolder[0]).into(imageViewPhoto);
        }

        // Handle removing the current photo
        btnRemovePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageViewPhoto.setVisibility(View.GONE);
                btnRemovePhoto.setVisibility(View.GONE);
                // Mark photo as removed by setting it to null in the array
                imageUrlHolder[0] = null;
            }
        });

        // Handle replacing the photo
        btnReplacePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPhotoOptions(); // Option to pick new photo (from gallery or camera)
            }
        });

        // Save the note with updated text or photo
        btnSaveNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newNoteText = editTextNote.getText().toString();

                // If a new photo is selected or replaced, upload it; otherwise use the existing image URL
                if (photoUri != null) {
                    saveEditedNoteToFirebase(userId, noteId, newNoteText, imageUrlHolder[0], photoUri);
                } else {
                    saveEditedNoteToFirebase(userId, noteId, newNoteText, imageUrlHolder[0], null);
                }

                dialog.dismiss();
            }
        });


        // Cancel the edit
        btnCancelNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    private void saveEditedNoteToFirebase(String userId, String noteId, String noteContent, String currentImageUrl, Uri newPhotoUri) {
        DatabaseReference noteRef = mDatabase.child(userId).child("notes").child(noteId);

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("text", noteContent);

        if (newPhotoUri != null) {
            // New photo selected, upload it
            uploadImageAndSaveNote(noteRef, noteId, noteData, newPhotoUri);
        } else {
            // Check if the photo was removed
            if (currentImageUrl == null) {
                noteData.put("imageUrl", null);  // Remove the image URL if user removed the photo
            }
            saveNoteData(noteRef, noteId, noteData); // Save the note data without uploading an image
        }
    }

    private void deleteNote() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // 執行刪除操作
                        performDeleteNote();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void performDeleteNote() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || selectedDate == null) {
            Toast.makeText(this, "Unable to delete note", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateKey = dateFormat.format(selectedDate);

        DatabaseReference noteRef = mDatabase.child(userId).child("notes").child(dateKey);

        noteRef.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(CalendarActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show();
                    updateRecentNotes(null, null);
                    showEditDeleteButtons(false);
                } else {
                    Toast.makeText(CalendarActivity.this, "Failed to delete note", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updatePredictions(Date startDate) {
        Log.d(TAG, "Updating predictions");
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);

        // 清除之前的所有事件
        calendarView.removeAllEvents();

        // 添加過去三個月的經期
        for (int i = -2; i <= 0; i++) {
            Calendar pastCal = (Calendar) cal.clone();
            pastCal.add(Calendar.MONTH, i);
            addPeriodToCalendar(pastCal.getTime(), averagePeriodLength);
        }

        // 添加當前經期（如果有）
        if (currentPeriodStartDate != null) {
            addPeriodToCalendar(currentPeriodStartDate, averagePeriodLength);
        }

        // 計算並添加下次經期
        Calendar nextPeriodCal = (Calendar) cal.clone();
        nextPeriodCal.add(Calendar.DAY_OF_MONTH, averageCycleLength);
        Date nextPeriodDate = nextPeriodCal.getTime();
        addPeriodToCalendar(nextPeriodDate, averagePeriodLength);

        // 添加未來兩個月的預測經期
        for (int i = 2; i <= 3; i++) {
            Calendar futureCal = (Calendar) cal.clone();
            futureCal.add(Calendar.MONTH, i);
            addPeriodToCalendar(futureCal.getTime(), averagePeriodLength);
        }

        // 更新UI文本
        textViewNextPeriod.setText(getString(R.string.next_period, dateFormatForDay.format(nextPeriodDate)));

        // 排卵日
        cal.setTime(startDate);
        cal.add(Calendar.DAY_OF_MONTH, averageCycleLength - 14);
        Date ovulationDate = cal.getTime();
        addEventToCalendar(ovulationDate, R.color.ovulation_color);
        textViewOvulation.setText(getString(R.string.ovulation, dateFormatForDay.format(ovulationDate)));

        // 中間痛
        addEventToCalendar(cal.getTime(), R.color.middle_pain);
        textViewMiddlePain.setText(getString(R.string.middle_pain, dateFormatForDay.format(cal.getTime())));

        // 黃體期
        Calendar lutealStart = (Calendar) cal.clone();
        lutealStart.add(Calendar.DAY_OF_MONTH, 1);
        Calendar lutealEnd = (Calendar) cal.clone();
        lutealEnd.add(Calendar.DAY_OF_MONTH, 14);
        addEventRangeToCalendar(lutealStart.getTime(), lutealEnd.getTime(), R.color.luteal_color);
        textViewLutealPhase.setText(getString(R.string.luteal_phase1,
                dateFormatForDay.format(lutealStart.getTime()),
                dateFormatForDay.format(lutealEnd.getTime())));

        // 卵泡期
        Calendar follicularStart = Calendar.getInstance();
        follicularStart.setTime(startDate);
        follicularStart.add(Calendar.DAY_OF_MONTH, averagePeriodLength);
        Calendar follicularEnd = (Calendar) cal.clone();
        follicularEnd.add(Calendar.DAY_OF_MONTH, -1);
        addEventRangeToCalendar(follicularStart.getTime(), follicularEnd.getTime(), R.color.follicular_color);
        textViewFollicularPhase.setText(getString(R.string.follicular_phase1,
                dateFormatForDay.format(follicularStart.getTime()),
                dateFormatForDay.format(follicularEnd.getTime())));

        calendarView.invalidate();
        Log.d(TAG, "Predictions updated and added to calendar");
    }

    private void addPeriodToCalendar(Date startDate, int length) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        for (int i = 0; i < length; i++) {
            Event ev = new Event(getResources().getColor(R.color.period_color), cal.getTimeInMillis());
            calendarView.addEvent(ev);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void addEventToCalendar(Date date, @ColorRes int colorRes) {
        Event event = new Event(getResources().getColor(colorRes), date.getTime());
        calendarView.addEvent(event);
    }

    private void addEventRangeToCalendar(Date startDate, Date endDate, @ColorRes int colorRes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        Date currentDate = cal.getTime();

        while (!currentDate.after(endDate)) {
            addEventToCalendar(currentDate, colorRes);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            currentDate = cal.getTime();
        }
    }

    private void startPeriod(Date date) {
        Log.d(TAG, "Starting period on date: " + dateFormatForDay.format(date));
        currentPeriodStartDate = date;

        // 更新 lastPeriodStartDate
        if (lastPeriodStartDate == null || date.after(lastPeriodStartDate)) {
            lastPeriodStartDate = currentPeriodStartDate;
        }

        updatePredictions(lastPeriodStartDate);
        saveLastPeriodDate(date);

        // 強制日曆重新繪製
        calendarView.removeAllEvents();
        updatePredictions(lastPeriodStartDate);
        calendarView.invalidate();

        Log.d(TAG, "Period started and saved");
    }

    private void endPeriod(Date date) {
        Log.d(TAG, "Ending period");
        if (currentPeriodStartDate != null) {
            long diff = date.getTime() - currentPeriodStartDate.getTime();
            int actualPeriodLength = (int) (diff / (24 * 60 * 60 * 1000)) + 1;
            averagePeriodLength = actualPeriodLength; // 更新平均經期長度
            savePeriodLength(String.valueOf(actualPeriodLength));
            lastPeriodStartDate = currentPeriodStartDate; // 更新上次經期開始日期
            updatePredictions(lastPeriodStartDate);
            currentPeriodStartDate = null;
            Log.d(TAG, "Period ended and data saved");
        } else {
            Log.e(TAG, "Attempted to end period but currentPeriodStartDate is null");
        }
    }

    private void saveLastPeriodDate(Date date) {
        Log.d(TAG, "Saving last period date: " + dateFormatForDay.format(date));
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            mDatabase.child(userId).child("lastPeriodDay").setValue(cal.get(Calendar.DAY_OF_MONTH));
            mDatabase.child(userId).child("lastPeriodMonth").setValue(cal.get(Calendar.MONTH));
            mDatabase.child(userId).child("lastPeriodYear").setValue(cal.get(Calendar.YEAR));
            Log.d(TAG, "Last period date saved to Firebase");
        } else {
            Log.e(TAG, "Failed to save last period date: current user is null");
        }
    }

    private void savePeriodLength(String length) {
        Log.d(TAG, "Saving period length: " + length);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            mDatabase.child(userId).child("periodLength").setValue(length);
            Log.d(TAG, "Period length saved to Firebase");
        } else {
            Log.e(TAG, "Failed to save period length: current user is null");
        }
    }

}