package com.example.calendarapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendarapp.databinding.ActivityDayEventsBinding;

import java.util.ArrayList;

public class DayEventsActivity extends AppCompatActivity {

    private ActivityDayEventsBinding binding;
    private EventViewModel eventViewModel;
    private EventAdapter adapter;
    private String date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDayEventsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Light status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        date = getIntent().getStringExtra("date");
        binding.dayHeader.setText("Schedule for " + date);

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        
        adapter = new EventAdapter(this, new ArrayList<>(), event -> {
            eventViewModel.delete(event);
        });

        binding.eventList.setAdapter(adapter);

        eventViewModel.getEventsByDate(date).observe(this, events -> {
            if (events != null && !events.isEmpty()) {
                adapter.updateData(events);
                binding.emptyStateLayout.setVisibility(View.GONE);
            } else {
                adapter.updateData(new ArrayList<>());
                binding.emptyStateLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
