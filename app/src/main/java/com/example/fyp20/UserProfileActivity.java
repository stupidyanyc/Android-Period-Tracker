package com.example.fyp20;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Calendar;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private TextView textViewUserId, textViewEmail, textViewAge, textViewBirthday, textViewLastPeriod, textViewPeriodLength, textViewCycleLength;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("User Profile");

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        initializeViews();
        setupButtons();
        setupBottomNavigation();
        loadUserData();
    }

    private void initializeViews() {
        textViewUserId = findViewById(R.id.textViewUserId);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewAge = findViewById(R.id.textViewAge);
        textViewBirthday = findViewById(R.id.textViewBirthday);
        textViewLastPeriod = findViewById(R.id.textViewLastPeriod);
        textViewPeriodLength = findViewById(R.id.textViewPeriodLength);
        textViewCycleLength = findViewById(R.id.textViewCycleLength);
    }

    private void setupButtons() {
        MaterialButton buttonOpenSettings = findViewById(R.id.buttonOpenSettings);
        buttonOpenSettings.setOnClickListener(v -> openSettings());

        //MaterialButton buttonMyCollection = findViewById(R.id.buttonMyCollection);
        //buttonMyCollection.setOnClickListener(v -> openMyCollection());
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    navigateToCalendar();
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
            Log.d("UserProfile", "Attempting to load data for user: " + userId);

            mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Log.d("UserProfile", "DataSnapshot exists. Raw data: " + dataSnapshot.getValue());
                        UserInfo userInfo = dataSnapshot.getValue(UserInfo.class);
                        if (userInfo != null) {
                            Log.d("UserProfile", "Successfully retrieved user data: " + userInfo.toString());
                            updateUI(userInfo);
                        } else {
                            Log.e("UserProfile", "Failed to convert DataSnapshot to UserInfo object");
                        }
                    } else {
                        Log.e("UserProfile", "DataSnapshot does not exist for user: " + userId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("UserProfile", "Database error: " + databaseError.getMessage());
                }
            });
        } else {
            Log.e("UserProfile", "Current user is null");
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

//    private void openMyCollection() {
//        Intent intent = new Intent(this, CollectionActivity.class);
//        startActivity(intent);
//    }
}