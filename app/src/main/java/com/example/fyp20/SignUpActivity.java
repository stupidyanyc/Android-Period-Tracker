package com.example.fyp20;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SignUpActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private DatePicker datePickerBirthday, datePickerLastPeriod;
    private Spinner spinnerPeriodLength, spinnerCycleLength;
    private Button buttonRegister;
    private TextView textViewLogin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        TextView textViewLogin = findViewById(R.id.textViewLogin);

        String text = "Already Have An Account? Login";
        SpannableString spannableString = new SpannableString(text);

        // 找到 "登錄" 的起始位置和结束位置
        int start = text.indexOf("Login");
        int end = start + "Login".length();

        // 为 "登錄" 添加下划线
        spannableString.setSpan(new UnderlineSpan(), start, end, 0);

        textViewLogin.setText(spannableString);

        initializeViews();
        setupSpinners();
        setupListeners();
    }

    private void initializeViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        datePickerBirthday = findViewById(R.id.datePickerBirthday);
        datePickerLastPeriod = findViewById(R.id.datePickerLastPeriod);
        spinnerPeriodLength = findViewById(R.id.spinnerPeriodLength);
        spinnerCycleLength = findViewById(R.id.spinnerCycleLength);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
    }

    private void setupSpinners() {
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"3", "4", "5", "6", "7"});
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriodLength.setAdapter(periodAdapter);

        ArrayAdapter<String> cycleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35"});
        cycleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCycleLength.setAdapter(cycleAdapter);
    }

    private void setupListeners() {
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    registerUser();
                }
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 跳轉到登錄頁面
                Toast.makeText(SignUpActivity.this, "跳轉到登錄頁面", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInput() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("請輸入有效的電子郵件地址");
            return false;
        }

        if (password.isEmpty() || password.length() < 6) {
            editTextPassword.setError("密碼長度至少為6位");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("密碼不匹配");
            return false;
        }

        return true;
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        int birthYear = datePickerBirthday.getYear();
        int birthMonth = datePickerBirthday.getMonth();
        int birthDay = datePickerBirthday.getDayOfMonth();

        int lastPeriodYear = datePickerLastPeriod.getYear();
        int lastPeriodMonth = datePickerLastPeriod.getMonth();
        int lastPeriodDay = datePickerLastPeriod.getDayOfMonth();

        String periodLength = spinnerPeriodLength.getSelectedItem().toString();
        String cycleLength = spinnerCycleLength.getSelectedItem().toString();

        // TODO: 實現用戶註冊邏輯，可能涉及網絡請求或本地數據庫操作

        Toast.makeText(this, "註冊成功！", Toast.LENGTH_SHORT).show();
    }
}