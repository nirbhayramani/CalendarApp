package com.example.calendarapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendarapp.databinding.ActivityMonthEventsBinding;

import java.util.ArrayList;

public class MonthEventsActivity extends AppCompatActivity {

    private ActivityMonthEventsBinding binding;
    private EventViewModel eventViewModel;
    private EventAdapter adapter;
    private String month;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMonthEventsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Light status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        month = getIntent().getStringExtra("month");
        binding.monthHeader.setText("Events in " + month);

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);

        adapter = new EventAdapter(this, new ArrayList<>(), event -> {
            eventViewModel.delete(event);
        });

        binding.monthEventList.setAdapter(adapter);

        eventViewModel.getEventsByMonth(month).observe(this, events -> {
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
