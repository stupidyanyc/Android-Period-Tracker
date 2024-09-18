package com.example.fyp20;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private TextInputEditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegister, textViewForgotPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        buttonLogin.setOnClickListener(v -> loginUser(v));

        textViewRegister.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class))
        );

        textViewForgotPassword.setOnClickListener(this::handleForgotPassword);
    }

    private void loginUser(View view) {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showError(editTextEmail, "Please Enter Email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showError(editTextPassword, "Please Enter Password");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Snackbar.make(view, "Login Succesfully", Snackbar.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, UserProfileActivity.class));
                finish();
            } else {
                Snackbar.make(view, "Login Failed: " + task.getException().getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void handleForgotPassword(View view) {
        String email = editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showError(editTextEmail, "Enter Email to Reset Password");
            return;
        }

        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Snackbar.make(view, "Reset Password Email Has Been Sent. Please Check Your Email", Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(view, "Failed to Sent Reset Password Email: " + task.getException().getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showError(View view, String message) {
        if (view instanceof EditText) {
            ((EditText) view).setError(message);
        }
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }
}