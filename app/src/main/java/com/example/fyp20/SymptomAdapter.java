package com.example.fyp20;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SymptomAdapter extends RecyclerView.Adapter<SymptomAdapter.SymptomViewHolder> {
    private List<Symptom> symptoms;

    public SymptomAdapter(List<Symptom> symptoms) {
        this.symptoms = symptoms;
    }

    @NonNull
    @Override
    public SymptomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_symptom, parent, false);
        return new SymptomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SymptomViewHolder holder, int position) {
        Symptom symptom = symptoms.get(position);
        holder.textViewSymptom.setText(symptom.getName());
        holder.textViewSeverity.setText("Severity: " + symptom.getSeverity());
    }

    @Override
    public int getItemCount() {
        return symptoms.size();
    }

    static class SymptomViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSymptom;
        TextView textViewSeverity;

        SymptomViewHolder(View itemView) {
            super(itemView);
            textViewSymptom = itemView.findViewById(R.id.textViewSymptom);
            textViewSeverity = itemView.findViewById(R.id.textViewSeverity);
        }
    }
}