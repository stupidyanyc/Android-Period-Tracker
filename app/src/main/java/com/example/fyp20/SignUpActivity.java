package com.example.fyp20;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;

public class SignUpActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private Button buttonNext;
    private Button buttonPrevious;
    private List<SignUpFragment> fragments;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextView textViewLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        buttonNext = findViewById(R.id.buttonNext);
        buttonPrevious = findViewById(R.id.buttonPrevious);
        textViewLogin = findViewById(R.id.textViewLogin);

        setupFragments();
        setupViewPager();
        setupButtons();
        setupLoginTextView();
    }

    private void setupFragments() {
        fragments = new ArrayList<>();
        fragments.add(new EmailPasswordFragment());
        fragments.add(new BirthdayFragment());
        fragments.add(new PeriodInfoFragment());
    }

    private void setupViewPager() {
        SignUpPagerAdapter pagerAdapter = new SignUpPagerAdapter(this, fragments);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(false);  // Disable swiping

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText("Step " + (position + 1))
        ).attach();
    }

    private void setupButtons() {
        buttonNext.setOnClickListener(v -> moveToNextPage());
        buttonPrevious.setOnClickListener(v -> moveToPreviousPage());
        updateButtonVisibility(0);
    }

    private void moveToNextPage() {
        int currentItem = viewPager.getCurrentItem();
        if (currentItem < fragments.size() - 1) {
            if (fragments.get(currentItem).isValid()) {
                viewPager.setCurrentItem(currentItem + 1);
                updateButtonVisibility(currentItem + 1);
            }
        } else {
            if (fragments.get(currentItem).isValid()) {
                registerUser();
            }
        }
    }

    private void moveToPreviousPage() {
        int currentItem = viewPager.getCurrentItem();
        if (currentItem > 0) {
            viewPager.setCurrentItem(currentItem - 1);
            updateButtonVisibility(currentItem - 1);
        }
    }

    private void updateButtonVisibility(int position) {
        buttonPrevious.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
        buttonNext.setText(position == fragments.size() - 1 ? "Register" : "Next");
    }

    private void registerUser() {
        EmailPasswordFragment emailPasswordFragment = (EmailPasswordFragment) fragments.get(0);
        BirthdayFragment birthdayFragment = (BirthdayFragment) fragments.get(1);
        PeriodInfoFragment periodInfoFragment = (PeriodInfoFragment) fragments.get(2);

        String email = emailPasswordFragment.getEmail();
        String password = emailPasswordFragment.getPassword();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            UserInfo userInfo = new UserInfo(
                                    userId,
                                    email,
                                    birthdayFragment.getBirthdayDay(),
                                    birthdayFragment.getBirthdayMonth(),
                                    birthdayFragment.getBirthdayYear(),
                                    periodInfoFragment.getLastPeriodDay(),
                                    periodInfoFragment.getLastPeriodMonth(),
                                    periodInfoFragment.getLastPeriodYear(),
                                    periodInfoFragment.getPeriodLength(),
                                    periodInfoFragment.getCycleLength()
                            );
                            mDatabase.child(userId).setValue(userInfo);

                            Snackbar.make(viewPager, "Register Succesfully", Snackbar.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                            finish();
                        }
                    } else {
                        Snackbar.make(viewPager, "Register Failed: " + task.getException().getMessage(),
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupLoginTextView() {
        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // 跳轉後關閉當前的SignUpActivity
            }
        });
    }
}