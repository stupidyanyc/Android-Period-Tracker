package com.example.fyp20;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private DatePicker datePickerBirthday, datePickerLastPeriod;
    private Spinner spinnerPeriodLength, spinnerCycleLength;
    private Button buttonRegister;
    private TextView textViewLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        datePickerBirthday = findViewById(R.id.datePickerBirthday);
        datePickerLastPeriod = findViewById(R.id.datePickerLastPeriod);
        spinnerPeriodLength = findViewById(R.id.spinnerPeriodLength);
        spinnerCycleLength = findViewById(R.id.spinnerCycleLength);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);

        // Set up the spinners to be clickable and interactable
        setupSpinners();

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser(v);
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            }
        });
    }

    private void setupSpinners() {
        // Setting up the period length spinner
        ArrayAdapter<CharSequence> periodLengthAdapter = ArrayAdapter.createFromResource(this,
                R.array.period_length_array, android.R.layout.simple_spinner_item);
        periodLengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriodLength.setAdapter(periodLengthAdapter);
        spinnerPeriodLength.setEnabled(true);
        spinnerPeriodLength.setClickable(true);

        // Setting up the cycle length spinner
        ArrayAdapter<CharSequence> cycleLengthAdapter = ArrayAdapter.createFromResource(this,
                R.array.cycle_length_array, android.R.layout.simple_spinner_item);
        cycleLengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCycleLength.setAdapter(cycleLengthAdapter);
        spinnerCycleLength.setEnabled(true);
        spinnerCycleLength.setClickable(true);
    }

    private void registerUser(View view) {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("請輸入電子郵件");
            Snackbar.make(view, "請輸入電子郵件", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("請輸入有效的電子郵件地址");
            Snackbar.make(view, "請輸入有效的電子郵件地址", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("請輸入密碼");
            Snackbar.make(view, "請輸入密碼", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!isValidPassword(password)) {
            editTextPassword.setError("密碼至少需包含8個字符，並包含至少一個大寫字母、一個小寫字母、一個數字和一個特殊符號");
            Snackbar.make(view, "密碼需包含8個字符，並至少有一個大寫字母、一個小寫字母、一個數字和一個特殊符號", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("密碼不一致");
            Snackbar.make(view, "密碼不一致", Snackbar.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    String userId = user.getUid();
                    UserInfo userInfo = new UserInfo(userId, email,
                            datePickerBirthday.getDayOfMonth(), datePickerBirthday.getMonth(), datePickerBirthday.getYear(),
                            datePickerLastPeriod.getDayOfMonth(), datePickerLastPeriod.getMonth(), datePickerLastPeriod.getYear(),
                            spinnerPeriodLength.getSelectedItem().toString(), spinnerCycleLength.getSelectedItem().toString());
                    mDatabase.child(userId).setValue(userInfo);

                    Snackbar.make(view, "註冊成功", Snackbar.LENGTH_SHORT).show();
                    startActivity(new Intent(SignUpActivity.this, UserProfileActivity.class));
                } else {
                    Snackbar.make(view, "註冊失敗: " + task.getException().getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isValidPassword(String password) {
        Pattern passwordPattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$");
        return passwordPattern.matcher(password).matches();
    }
}
