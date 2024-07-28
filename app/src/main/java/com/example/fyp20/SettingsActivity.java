package com.example.fyp20;

import androidx.appcompat.app.AppCompatActivity;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private TextView textViewReminderTime;
    private TextView textViewWaterReminderTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        textViewReminderTime = findViewById(R.id.textViewReminderTime);
        textViewWaterReminderTime = findViewById(R.id.textViewWaterReminderTime);

        Button buttonSetReminderTime = findViewById(R.id.buttonSetReminderTime);
        buttonSetReminderTime.setOnClickListener(v -> showTimePickerDialog(true));

        Button buttonSetWaterReminderTime = findViewById(R.id.buttonSetWaterReminderTime);
        buttonSetWaterReminderTime.setOnClickListener(v -> showTimePickerDialog(false));
    }

    private void showTimePickerDialog(boolean isReminderTime) {
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                    if (isReminderTime) {
                        textViewReminderTime.setText(time);
                        // TODO: 保存經期提醒時間
                    } else {
                        textViewWaterReminderTime.setText(time);
                        // TODO: 保存喝水提醒時間
                    }
                }, hour, minute, true);

        timePickerDialog.show();
    }
}