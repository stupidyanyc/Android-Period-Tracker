package com.example.fyp20;

public class Symptom {
    private String name;
    private int severity;

    public Symptom() {
        // Default constructor required for calls to DataSnapshot.getValue(Symptom.class)
    }

    public Symptom(String name, int severity) {
        this.name = name;
        this.severity = severity;
    }

    public String getName() {
        return name;
    }

    public int getSeverity() {
        return severity;
    }
}