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
            showError(editTextEmail, "Please Enter Email");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(editTextEmail, "Please Enter Valid Email");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            showError(passwordLayout, "Please Enter Password");
            return false;
        }
        if (!isValidPassword(password)) {
            showError(passwordLayout, "The password must contain 8 characters and have at least one uppercase letter, one lowercase letter, one number and one special symbol");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showError(confirmPasswordLayout, "Password Not Match");
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