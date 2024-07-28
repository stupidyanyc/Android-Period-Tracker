package com.example.fyp20;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

public class LoginActivity extends AppCompatActivity {

    TextView textViewRegister = findViewById(R.id.textViewRegister);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        String text = "Don't Have An Account Yet? Register";
        SpannableString spannableString = new SpannableString(text);

        // 找到 "登錄" 的起始位置和结束位置
        int start = text.indexOf("Register");
        int end = start + "Register".length();

        // 为 "登錄" 添加下划线
        spannableString.setSpan(new UnderlineSpan(), start, end, 0);

        textViewRegister.setText(spannableString);
    }
}