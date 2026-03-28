package com.example.calendarapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calendarapp.databinding.ItemEventBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final Context context;
    private final List<EventModel> events;
    private final OnEventActionListener actionListener;

    public interface OnEventActionListener {
        void onDelete(EventModel event);
    }

    public EventAdapter(Context context, List<EventModel> events, OnEventActionListener listener) {
        this.context = context;
        this.events = events;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEventBinding binding = ItemEventBinding.inflate(LayoutInflater.from(context), parent, false);
        return new EventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventModel event = events.get(position);
        holder.binding.timeText.setText(event.time);
        holder.binding.titleText.setText(event.title);

        // Priority visualization
        int priorityColor;
        switch (event.priority) {
            case 2: // High
                priorityColor = ContextCompat.getColor(context, R.color.error);
                break;
            case 1: // Medium
                priorityColor = ContextCompat.getColor(context, R.color.accent);
                break;
            default: // Low
                priorityColor = ContextCompat.getColor(context, R.color.text_hint);
                break;
        }
        holder.binding.priorityIndicator.setBackgroundTintList(ColorStateList.valueOf(priorityColor));

        // Reminder style icon
        if (event.reminderType == 1) { // Alarm
            holder.binding.reminderIcon.setVisibility(View.VISIBLE);
        } else {
            holder.binding.reminderIcon.setVisibility(View.GONE);
        }

        holder.binding.editBtn.setOnClickListener(v -> {
            Intent i = new Intent(context, EditEventActivity.class);
            i.putExtra("id", event.id);
            i.putExtra("date", event.date);
            i.putExtra("time", event.time);
            i.putExtra("title", event.title);
            i.putExtra("priority", event.priority);
            i.putExtra("reminderType", event.reminderType);
            context.startActivity(i);
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        holder.binding.deleteBtn.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to remove this event from your schedule?")
                    .setPositiveButton("Delete", (d, w) -> {
                        if (actionListener != null) {
                            actionListener.onDelete(event);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    public void updateData(List<EventModel> newEvents) {
        this.events.clear();
        if (newEvents != null) {
            this.events.addAll(newEvents);
        }
        notifyDataSetChanged();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        ItemEventBinding binding;

        public EventViewHolder(ItemEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
