package com.example.fyp20;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserProfileActivity extends AppCompatActivity {

    private TextView textViewUserId, textViewEmail, textViewBirthday, textViewLastPeriod, textViewPeriodLength, textViewCycleLength;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        textViewUserId = findViewById(R.id.textViewUserId);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewBirthday = findViewById(R.id.textViewBirthday);
        textViewLastPeriod = findViewById(R.id.textViewLastPeriod);
        textViewPeriodLength = findViewById(R.id.textViewPeriodLength);
        textViewCycleLength = findViewById(R.id.textViewCycleLength);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            mDatabase.child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    UserInfo userInfo = dataSnapshot.getValue(UserInfo.class);
                    if (userInfo != null) {
                        textViewUserId.setText(userInfo.getUserId());
                        textViewEmail.setText(userInfo.getEmail());
                        textViewBirthday.setText("生日: " + userInfo.getBirthdayDay() + "/" + (userInfo.getBirthdayMonth() + 1) + "/" + userInfo.getBirthdayYear());
                        textViewLastPeriod.setText("上次經期: " + userInfo.getLastPeriodDay() + "/" + (userInfo.getLastPeriodMonth() + 1) + "/" + userInfo.getLastPeriodYear());
                        textViewPeriodLength.setText("經期長度: " + userInfo.getPeriodLength());
                        textViewCycleLength.setText("週期長度: " + userInfo.getCycleLength());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle possible errors.
                }
            });
        }
    }
}
