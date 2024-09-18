package com.example.fyp20;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.FirebaseNetworkException;

import java.util.Calendar;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    private TextView textViewUserId, textViewEmail, textViewAge, textViewBirthday, textViewLastPeriod, textViewPeriodLength, textViewCycleLength;
    private ImageView profileImage;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        setupToolbar();
        initializeFirebase();
        initializeViews();
        setupButtons();
        setupBottomNavigation();
        loadUserData();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("User Profile");
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");
        mStorageRef = FirebaseStorage.getInstance().getReference("profile_images");
    }

    private void initializeViews() {
        textViewUserId = findViewById(R.id.textViewUserId);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewAge = findViewById(R.id.textViewAge);
        textViewBirthday = findViewById(R.id.textViewBirthday);
        textViewLastPeriod = findViewById(R.id.textViewLastPeriod);
        textViewPeriodLength = findViewById(R.id.textViewPeriodLength);
        textViewCycleLength = findViewById(R.id.textViewCycleLength);
        profileImage = findViewById(R.id.profileImage);
    }

    private void setupButtons() {
        MaterialButton buttonOpenSettings = findViewById(R.id.buttonOpenSettings);
        buttonOpenSettings.setOnClickListener(v -> openSettings());

        MaterialButton buttonChangeProfilePhoto = findViewById(R.id.buttonChangeProfilePhoto);
        buttonChangeProfilePhoto.setOnClickListener(v -> changeProfilePhoto());

        MaterialButton buttonChangeUsername = findViewById(R.id.buttonChangeUsername);
        buttonChangeUsername.setOnClickListener(v -> changeUsername());

        MaterialButton buttonChangeEmail = findViewById(R.id.buttonChangeEmail);
        buttonChangeEmail.setOnClickListener(v -> changeEmail());

        MaterialButton buttonChangePassword = findViewById(R.id.buttonChangePassword);
        buttonChangePassword.setOnClickListener(v -> changePassword());
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    navigateToCalendar();
                    return true;
                case R.id.navigation_forum:
                    navigateToForum();
                    return true;
                case R.id.navigation_profile:
                    // Already on profile
                    return true;
                case R.id.navigation_settings:
                    openSettings();
                    return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            Log.d(TAG, "Attempting to load data for user: " + userId);

            mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Log.d(TAG, "DataSnapshot exists. Raw data: " + dataSnapshot.getValue());
                        UserInfo userInfo = dataSnapshot.getValue(UserInfo.class);
                        if (userInfo != null) {
                            Log.d(TAG, "Successfully retrieved user data: " + userInfo.toString());
                            updateUI(userInfo);
                        } else {
                            Log.e(TAG, "Failed to convert DataSnapshot to UserInfo object");
                        }
                    } else {
                        Log.e(TAG, "DataSnapshot does not exist for user: " + userId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Database error: " + databaseError.getMessage());
                }
            });
        } else {
            Log.e(TAG, "Current user is null");
        }
    }

    private void updateUI(UserInfo userInfo) {
        textViewUserId.setText("User ID: " + userInfo.getUserId());
        textViewEmail.setText("Email: " + userInfo.getEmail());

        int age = calculateAge(userInfo.getBirthdayYear(), userInfo.getBirthdayMonth(), userInfo.getBirthdayDay());
        textViewAge.setText("Age: " + age + " Years Old");

        String birthdayStr = String.format(Locale.getDefault(), "%d/%d/%d",
                userInfo.getBirthdayDay(), userInfo.getBirthdayMonth() + 1, userInfo.getBirthdayYear());
        textViewBirthday.setText("Birthday: " + birthdayStr);

        String lastPeriodStr = String.format(Locale.getDefault(), "%d/%d/%d",
                userInfo.getLastPeriodDay(), userInfo.getLastPeriodMonth() + 1, userInfo.getLastPeriodYear());
        textViewLastPeriod.setText("Last Period: " + lastPeriodStr);

        textViewPeriodLength.setText("Period Length: " + userInfo.getPeriodLength() + " days");
        textViewCycleLength.setText("Cycle Length: " + userInfo.getCycleLength() + " days");

        // Load profile image
        if (userInfo.getProfileImageUrl() != null && !userInfo.getProfileImageUrl().isEmpty()) {
            Glide.with(this).load(userInfo.getProfileImageUrl()).into(profileImage);
        }
    }

    private void changeProfilePhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadProfileImage(imageUri);
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            StorageReference fileReference = mStorageRef.child(user.getUid() + ".jpg");

            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        updateProfileImageUrl(imageUrl);
                        Glide.with(UserProfileActivity.this).load(imageUrl).into(profileImage);
                    }))
                    .addOnFailureListener(e -> Toast.makeText(UserProfileActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show());
        }
    }

    private void updateProfileImageUrl(String imageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mDatabase.child(user.getUid()).child("profileImageUrl").setValue(imageUrl);
        }
    }

    private void changeUsername() {
        showInputDialog("Change Username", "Enter new username", InputType.TYPE_CLASS_TEXT, newUsername -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(newUsername)
                        .build();

                user.updateProfile(profileUpdates)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                mDatabase.child(user.getUid()).child("userId").setValue(newUsername);
                                textViewUserId.setText("User ID: " + newUsername);
                                Toast.makeText(UserProfileActivity.this, "Username updated successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(UserProfileActivity.this, "Failed to update username", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void changeEmail() {
        showInputDialog("Change Email", "Enter new email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, newEmail -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                // Check if the new email is different from the current one
                if (user.getEmail() != null && user.getEmail().equals(newEmail)) {
                    Toast.makeText(UserProfileActivity.this, "New email is the same as the current one", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Send verification email to the new address
                user.verifyBeforeUpdateEmail(newEmail)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(UserProfileActivity.this, "Verification email sent to " + newEmail, Toast.LENGTH_LONG).show();
                                showVerificationInstructions(newEmail);
                            } else {
                                Log.e(TAG, "Failed to send verification email", task.getException());
                                handleUpdateEmailError(task.getException());
                            }
                        });
            } else {
                Log.e(TAG, "Current user is null");
                Toast.makeText(UserProfileActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showVerificationInstructions(String newEmail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Email Verification Sent");
        builder.setMessage("A verification email has been sent to " + newEmail + ". Please check your email and follow the instructions to verify your new email address. After verification, sign out and sign in again to complete the email change process.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void handleUpdateEmailError(Exception exception) {
        String errorMessage = "Failed to send verification email";
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            errorMessage = "Invalid email format";
        } else if (exception instanceof FirebaseAuthUserCollisionException) {
            errorMessage = "This email is already in use by another account";
        } else if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
            errorMessage = "Please log out and log back in before changing your email";
        } else if (exception instanceof FirebaseNetworkException) {
            errorMessage = "Network error. Please check your internet connection";
        } else {
            errorMessage = "Failed to send verification email: " + exception.getMessage();
        }
        Log.e(TAG, errorMessage, exception);
        Toast.makeText(UserProfileActivity.this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void changePassword() {
        showInputDialog("Change Password", "Enter new password", InputType.TYPE_TEXT_VARIATION_PASSWORD, newPassword -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                user.updatePassword(newPassword)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(UserProfileActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(UserProfileActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void showInputDialog(String title, String message, int inputType, OnInputListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        final EditText input = new EditText(this);
        input.setInputType(inputType);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String value = input.getText().toString();
            listener.onInput(value);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private interface OnInputListener {
        void onInput(String input);
    }

    private int calculateAge(int year, int month, int day) {
        Calendar dob = Calendar.getInstance();
        Calendar today = Calendar.getInstance();

        dob.set(year, month, day);

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        return age;
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void navigateToCalendar() {
        Intent intent = new Intent(this, CalendarActivity.class);
        startActivity(intent);
    }

    private void navigateToForum() {
         Intent intent = new Intent(this, ForumActivity.class);
         startActivity(intent);
    }
}