package com.example.fyp20;

public class UserInfo {
    private String userId;
    private String email;
    private int birthdayDay;
    private int birthdayMonth;
    private int birthdayYear;
    private int lastPeriodDay;
    private int lastPeriodMonth;
    private int lastPeriodYear;
    private String periodLength;
    private String cycleLength;

    public UserInfo() {
        // Default constructor required for calls to DataSnapshot.getValue(UserProfile.class)
    }

    public UserInfo(String userId, String email, int birthdayDay, int birthdayMonth, int birthdayYear,
                       int lastPeriodDay, int lastPeriodMonth, int lastPeriodYear, String periodLength, String cycleLength) {
        this.userId = userId;
        this.email = email;
        this.birthdayDay = birthdayDay;
        this.birthdayMonth = birthdayMonth;
        this.birthdayYear = birthdayYear;
        this.lastPeriodDay = lastPeriodDay;
        this.lastPeriodMonth = lastPeriodMonth;
        this.lastPeriodYear = lastPeriodYear;
        this.periodLength = periodLength;
        this.cycleLength = cycleLength;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public int getBirthdayDay() {
        return birthdayDay;
    }

    public int getBirthdayMonth() {
        return birthdayMonth;
    }

    public int getBirthdayYear() {
        return birthdayYear;
    }

    public int getLastPeriodDay() {
        return lastPeriodDay;
    }

    public int getLastPeriodMonth() {
        return lastPeriodMonth;
    }

    public int getLastPeriodYear() {
        return lastPeriodYear;
    }

    public String getPeriodLength() {
        return periodLength;
    }

    public String getCycleLength() {
        return cycleLength;
    }
}
