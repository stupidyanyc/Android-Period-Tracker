package com.example.fyp20;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    private Spinner spinnerLanguage;
    private Switch switchReminder, switchWaterReminder;
    private TextView textViewReminderTime1, textViewReminderTime2, textViewWaterReminderTime;
    private Button buttonSetReminderTime1, buttonSetReminderTime2, buttonSetWaterReminderTime;
    private Button buttonLogout, buttonDeleteAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");
        currentUser = mAuth.getCurrentUser();

        initializeViews();
        setupListeners();
        loadUserSettings();
    }

    private void initializeViews() {
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        switchReminder = findViewById(R.id.switchReminder);
        switchWaterReminder = findViewById(R.id.switchWaterReminder);
        textViewReminderTime1 = findViewById(R.id.textViewReminderTime1);
        textViewReminderTime2 = findViewById(R.id.textViewReminderTime2);
        textViewWaterReminderTime = findViewById(R.id.textViewWaterReminderTime);
        buttonSetReminderTime1 = findViewById(R.id.buttonSetReminderTime1);
        buttonSetReminderTime2 = findViewById(R.id.buttonSetReminderTime2);
        buttonSetWaterReminderTime = findViewById(R.id.buttonSetWaterReminderTime);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount);

        // Setup language spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
    }

    private void setupListeners() {
        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        switchWaterReminder.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        buttonSetReminderTime1.setOnClickListener(v -> showTimePickerDialog(textViewReminderTime1));
        buttonSetReminderTime2.setOnClickListener(v -> showTimePickerDialog(textViewReminderTime2));
        buttonSetWaterReminderTime.setOnClickListener(v -> setWaterReminderInterval());
        buttonLogout.setOnClickListener(v -> logout());
        buttonDeleteAccount.setOnClickListener(v -> deleteAccount());
    }

    private void loadUserSettings() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            mDatabase.child(userId).child("settings").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Map<String, Object> settings = (Map<String, Object>) dataSnapshot.getValue();
                        if (settings != null) {
                            spinnerLanguage.setSelection(((ArrayAdapter)spinnerLanguage.getAdapter()).getPosition(settings.get("language")));
                            switchReminder.setChecked((Boolean) settings.get("reminderEnabled"));
                            switchWaterReminder.setChecked((Boolean) settings.get("waterReminderEnabled"));
                            textViewReminderTime1.setText((String) settings.get("reminderTime1"));
                            textViewReminderTime2.setText((String) settings.get("reminderTime2"));
                            textViewWaterReminderTime.setText((String) settings.get("waterReminderInterval"));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Snackbar.make(findViewById(android.R.id.content), "Failed to load settings", Snackbar.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveSettings() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Map<String, Object> settings = new HashMap<>();
            settings.put("language", spinnerLanguage.getSelectedItem().toString());
            settings.put("reminderEnabled", switchReminder.isChecked());
            settings.put("waterReminderEnabled", switchWaterReminder.isChecked());
            settings.put("reminderTime1", textViewReminderTime1.getText().toString());
            settings.put("reminderTime2", textViewReminderTime2.getText().toString());
            settings.put("waterReminderInterval", textViewWaterReminderTime.getText().toString());

            mDatabase.child(userId).child("settings").setValue(settings)
                    .addOnSuccessListener(aVoid -> Snackbar.make(findViewById(android.R.id.content), "Settings saved", Snackbar.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Snackbar.make(findViewById(android.R.id.content), "Failed to save settings", Snackbar.LENGTH_SHORT).show());
        }
    }

    private void showTimePickerDialog(final TextView textView) {
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(SettingsActivity.this,
                (view, hourOfDay, minuteOfDay) -> {
                    textView.setText(String.format("%02d:%02d", hourOfDay, minuteOfDay));
                    saveSettings();
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void setWaterReminderInterval() {
        final String[] intervals = {"1 hour", "2 hours", "3 hours", "4 hours"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Water Reminder Interval")
                .setItems(intervals, (dialog, which) -> {
                    textViewWaterReminderTime.setText("Every" + intervals[which]);
                    saveSettings();
                });
        builder.create().show();
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
        finish();
    }

    private void deleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (currentUser != null) {
                        mDatabase.child(currentUser.getUid()).removeValue();
                        currentUser.delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Snackbar.make(findViewById(android.R.id.content), "Account deleted successfully", Snackbar.LENGTH_SHORT).show();
                                        startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                                        finish();
                                    } else {
                                        Snackbar.make(findViewById(android.R.id.content), "Failed to delete account", Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}