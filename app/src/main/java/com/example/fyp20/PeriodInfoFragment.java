// PeriodInfoFragment.java
package com.example.fyp20;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PeriodInfoFragment extends SignUpFragment {
    private DatePicker datePickerLastPeriod;
    private Spinner spinnerPeriodLength;
    private Spinner spinnerCycleLength;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_period_info, container, false);
        datePickerLastPeriod = view.findViewById(R.id.datePickerLastPeriod);
        spinnerPeriodLength = view.findViewById(R.id.spinnerPeriodLength);
        spinnerCycleLength = view.findViewById(R.id.spinnerCycleLength);

        setupSpinners();
        return view;
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> periodLengthAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.period_length_array, android.R.layout.simple_spinner_item);
        periodLengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriodLength.setAdapter(periodLengthAdapter);

        ArrayAdapter<CharSequence> cycleLengthAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.cycle_length_array, android.R.layout.simple_spinner_item);
        cycleLengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCycleLength.setAdapter(cycleLengthAdapter);
    }

    @Override
    public boolean isValid() {
        return true;  // Add validation if needed
    }

    public int getLastPeriodDay() {
        return datePickerLastPeriod.getDayOfMonth();
    }

    public int getLastPeriodMonth() {
        return datePickerLastPeriod.getMonth();
    }

    public int getLastPeriodYear() {
        return datePickerLastPeriod.getYear();
    }

    public String getPeriodLength() {
        return spinnerPeriodLength.getSelectedItem().toString();
    }

    public String getCycleLength() {
        return spinnerCycleLength.getSelectedItem().toString();
    }
}