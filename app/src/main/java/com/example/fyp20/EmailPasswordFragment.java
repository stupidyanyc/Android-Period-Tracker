package com.example.fyp20;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.regex.Pattern;

public class EmailPasswordFragment extends SignUpFragment {
    private EditText editTextEmail;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextConfirmPassword;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_email_password, container, false);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        passwordLayout = view.findViewById(R.id.passwordLayout);
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordLayout);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        editTextConfirmPassword = view.findViewById(R.id.editTextConfirmPassword);
        return view;
    }

    @Override
    public boolean isValid() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showError(editTextEmail, "請輸入電子郵件");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(editTextEmail, "請輸入有效的電子郵件地址");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            showError(passwordLayout, "請輸入密碼");
            return false;
        }
        if (!isValidPassword(password)) {
            showError(passwordLayout, "密碼至少需包含8個字符，並包含至少一個大寫字母、一個小寫字母、一個數字和一個特殊符號");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showError(confirmPasswordLayout, "密碼不一致");
            return false;
        }

        return true;
    }

    private void showError(View view, String message) {
        if (view instanceof EditText) {
            ((EditText) view).setError(message);
        } else if (view instanceof TextInputLayout) {
            ((TextInputLayout) view).setError(message);
        }
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    private boolean isValidPassword(String password) {
        Pattern passwordPattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$");
        return passwordPattern.matcher(password).matches();
    }

    public String getEmail() {
        return editTextEmail.getText().toString().trim();
    }

    public String getPassword() {
        return editTextPassword.getText().toString().trim();
    }
}