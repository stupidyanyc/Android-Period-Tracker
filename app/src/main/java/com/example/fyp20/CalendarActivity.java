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

    private Date selectedDate;
    private Date lastPeriodStartDate;
    private Date currentPeriodStartDate;
    private int averageCycleLength = 28;
    private int averagePeriodLength = 5;
    private String dateSelect;

    private SimpleDateFormat dateFormatForMonth = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
    private SimpleDateFormat dateFormatForDay = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static final int REQUEST_IMAGE_CAPTURE = 1;
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
            EditText noteEditText = dialogView.findViewById(R.id.editTextNote);
            String noteText = noteEditText.getText().toString();

            if (requestCode == CAMERA_REQUEST_CODE) {
                if (data != null && data.getExtras() != null) {
                    Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                    if (imageBitmap != null) {
                        ImageView imageView = dialogView.findViewById(R.id.imageViewPhoto);
                        imageView.setImageBitmap(imageBitmap);
                        imageView.setVisibility(View.VISIBLE);

                        // 上傳照片和文字
                        uploadPhotoAndSaveNote(imageBitmap, noteText);
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                if (data != null && data.getData() != null) {
                    Uri photoUri = data.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                        ImageView imageView = dialogView.findViewById(R.id.imageViewPhoto);
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);

                        // 上傳照片和文字
                        uploadPhotoAndSaveNote(bitmap, noteText);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
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

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = dateFormat.format(selectedDate);

        DatabaseReference userNotesRef = mDatabase.child(userId).child("notes").child(formattedDate);

        checkExistingNote(userNotesRef, (existingNoteId, existingNoteData) -> {
            Map<String, Object> updatedNoteData = new HashMap<>();

            if (existingNoteData != null) {
                // 保留現有的文本和圖片URL
                if (existingNoteData.containsKey("text")) {
                    updatedNoteData.put("text", existingNoteData.get("text"));
                }
                if (existingNoteData.containsKey("imageUrl")) {
                    updatedNoteData.put("imageUrl", existingNoteData.get("imageUrl"));
                }
            }

            // 更新文本（如果提供了新文本）
            if (noteText != null && !noteText.trim().isEmpty()) {
                updatedNoteData.put("text", noteText);
            }

            // 更新圖片URL（如果提供了新圖片URL）
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                updatedNoteData.put("imageUrl", imageUrl);
            }

            updatedNoteData.put("timestamp", ServerValue.TIMESTAMP);

            String noteId = existingNoteId != null ? existingNoteId : userNotesRef.push().getKey();

            if (noteId != null) {
                userNotesRef.child(noteId).updateChildren(updatedNoteData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(CalendarActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                            updateRecentNotes((String) updatedNoteData.get("text"), (String) updatedNoteData.get("imageUrl"));
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(CalendarActivity.this, "Failed to save note", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error saving note", e);
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

    private void saveImageUrlToFirebase(String imageUrl) {
        // Assuming you want to save the image URL along with the note
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            // Store the image URL under the user's notes
            DatabaseReference userNotesRef = mDatabase.child(userId).child("notes").child(dateSelect);
            String noteId = userNotesRef.push().getKey();
            Map<String, Object> noteData = new HashMap<>();
            noteData.put("imageUrl", imageUrl);
            noteData.put("timestamp", System.currentTimeMillis());

            if (noteId != null) {
                userNotesRef.child(noteId).updateChildren(noteData);
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

        selectedDate = new Date();
        calendarView.setCurrentDate(selectedDate);
        textViewDate.setText(dateFormatForMonth.format(selectedDate));
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
                            break;  // 只顯示最近的一條筆記
                        }
                    } else {
                        updateRecentNotes(null, null);
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
        ImageView imageViewPhoto = dialogView.findViewById(R.id.imageViewPhoto);

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