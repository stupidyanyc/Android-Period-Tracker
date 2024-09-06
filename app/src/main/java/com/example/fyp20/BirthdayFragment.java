// BirthdayFragment.java
package com.example.fyp20;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BirthdayFragment extends SignUpFragment {
    private DatePicker datePickerBirthday;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_birthday, container, false);
        datePickerBirthday = view.findViewById(R.id.datePickerBirthday);
        return view;
    }

    @Override
    public boolean isValid() {
        // You can add age validation here if needed
        return true;
    }

    public int getBirthdayDay() {
        return datePickerBirthday.getDayOfMonth();
    }

    public int getBirthdayMonth() {
        return datePickerBirthday.getMonth();
    }

    public int getBirthdayYear() {
        return datePickerBirthday.getYear();
    }
}
