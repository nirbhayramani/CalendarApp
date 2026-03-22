package com.example.calendarapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.calendarapp.databinding.ActivityMainBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Calendar selectedCalendar;
    private EventViewModel eventViewModel;
    private EventAdapter agendaAdapter;
    private CalendarAdapter calendarAdapter;
    private final int START_POSITION = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        selectedCalendar = Calendar.getInstance(); 
        
        setupFixedHeader();
        setupCustomCalendar();
        setupAgendaList();
        updateUI(selectedCalendar);

        binding.addEventBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            intent.putExtra("date", getFormattedDate(selectedCalendar));
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        binding.currentMonthText.setOnClickListener(v -> showMonthSelector());
        binding.currentYearText.setOnClickListener(v -> showYearSelector());
        
        binding.viewAllBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MonthEventsActivity.class);
            intent.putExtra("month", getMonthKey(selectedCalendar));
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void setupFixedHeader() {
        Calendar today = Calendar.getInstance();
        binding.todayDateText.setText(new SimpleDateFormat("dd MMMM", Locale.getDefault()).format(today.getTime()));
        
        binding.todayHeader.setOnClickListener(v -> {
            selectedCalendar = Calendar.getInstance();
            binding.calendarViewPager.setCurrentItem(START_POSITION, true);
            updateUI(selectedCalendar);
        });
    }

    private void setupCustomCalendar() {
        calendarAdapter = new CalendarAdapter(this, selectedCalendar, (calendar, isCurrentMonth) -> {
            selectedCalendar = (Calendar) calendar.clone();
            if (!isCurrentMonth) {
                navigateToMonth(selectedCalendar);
            }
            updateUI(selectedCalendar);
        });
        
        binding.calendarViewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        binding.calendarViewPager.setAdapter(calendarAdapter);
        binding.calendarViewPager.setCurrentItem(START_POSITION, false);

        binding.calendarViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Calendar monthCal = (Calendar) Calendar.getInstance().clone();
                monthCal.set(Calendar.DAY_OF_MONTH, 1);
                monthCal.add(Calendar.MONTH, position - START_POSITION);
                updateCalendarHeaderText(monthCal);
            }
        });
    }

    private void setupAgendaList() {
        agendaAdapter = new EventAdapter(this, new ArrayList<>(), event -> {
            eventViewModel.delete(event);
        });
        binding.agendaList.setAdapter(agendaAdapter);
    }

    private void updateUI(Calendar calendar) {
        updateCalendarHeaderText(calendar);
        calendarAdapter.setSelectedDate(calendar);

        String dateKey = getFormattedDate(calendar);
        eventViewModel.getEventsByDate(dateKey).observe(this, events -> {
            if (events != null && !events.isEmpty()) {
                agendaAdapter.updateData(events);
                binding.emptyAgendaLayout.setVisibility(View.GONE);
                binding.agendaList.setVisibility(View.VISIBLE);
            } else {
                agendaAdapter.updateData(new ArrayList<>());
                binding.emptyAgendaLayout.setVisibility(View.VISIBLE);
                binding.agendaList.setVisibility(View.GONE);
            }
        });
    }

    private void updateCalendarHeaderText(Calendar calendar) {
        binding.currentMonthText.setText(new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime()));
        binding.currentYearText.setText(String.valueOf(calendar.get(Calendar.YEAR)));
    }

    private void navigateToMonth(Calendar target) {
        Calendar now = Calendar.getInstance();
        int monthDiff = (target.get(Calendar.YEAR) - now.get(Calendar.YEAR)) * 12 + 
                        (target.get(Calendar.MONTH) - now.get(Calendar.MONTH));
        binding.calendarViewPager.setCurrentItem(START_POSITION + monthDiff, true);
    }

    private void showMonthSelector() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_month_selector, null);
        RecyclerView grid = view.findViewById(R.id.monthGrid);
        
        String[] months = {"January", "February", "March", "April", "May", "June", 
                          "July", "August", "September", "October", "November", "December"};
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Select Month")
                .setView(view)
                .create();

        MonthGridAdapter adapter = new MonthGridAdapter(months, position -> {
            selectedCalendar.set(Calendar.MONTH, position);
            navigateToMonth(selectedCalendar);
            updateUI(selectedCalendar);
            dialog.dismiss();
        });
        grid.setAdapter(adapter);
        dialog.show();
    }

    private void showYearSelector() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_year_selector, null);
        TextView header = view.findViewById(R.id.todayYearHeader);
        RecyclerView grid = view.findViewById(R.id.yearGrid);
        View inputLayout = view.findViewById(R.id.yearInputLayout);
        EditText edit = view.findViewById(R.id.yearEditText);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        header.setText(String.valueOf(currentYear));

        List<Integer> years = new ArrayList<>();
        for(int i = currentYear - 50; i <= currentYear + 50; i++) years.add(i);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Select Year")
                .setView(view)
                .setPositiveButton("Go", (d, w) -> {
                    if (inputLayout.getVisibility() == View.VISIBLE) {
                        try {
                            int y = Integer.parseInt(edit.getText().toString());
                            selectedCalendar.set(Calendar.YEAR, y);
                            navigateToMonth(selectedCalendar);
                            updateUI(selectedCalendar);
                        } catch (Exception ignored) {}
                    }
                })
                .create();

        header.setOnClickListener(v -> {
            header.setVisibility(View.GONE);
            inputLayout.setVisibility(View.VISIBLE);
            edit.requestFocus();
        });

        YearGridAdapter adapter = new YearGridAdapter(years, year -> {
            selectedCalendar.set(Calendar.YEAR, year);
            navigateToMonth(selectedCalendar);
            updateUI(selectedCalendar);
            dialog.dismiss();
        });
        grid.setAdapter(adapter);
        dialog.show();
    }

    private String getFormattedDate(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
    }

    private String getMonthKey(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.getTime());
    }

    // Temporary adapters for grid selectors
    private static class MonthGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        String[] months;
        OnItemClickListener listener;
        interface OnItemClickListener { void onClick(int pos); }
        MonthGridAdapter(String[] m, OnItemClickListener l) { months = m; listener = l; }
        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
            TextView tv = new TextView(p.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(-1, 150));
            tv.setGravity(17);
            return new RecyclerView.ViewHolder(tv) {};
        }
        @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int p) {
            ((TextView)h.itemView).setText(months[p]);
            h.itemView.setOnClickListener(v -> listener.onClick(p));
        }
        @Override public int getItemCount() { return 12; }
    }

    private static class YearGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        List<Integer> years;
        OnYearClickListener listener;
        interface OnYearClickListener { void onClick(int year); }
        YearGridAdapter(List<Integer> y, OnYearClickListener l) { years = y; listener = l; }
        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int t) {
            TextView tv = new TextView(p.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(-1, 150));
            tv.setGravity(17);
            return new RecyclerView.ViewHolder(tv) {};
        }
        @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int p) {
            ((TextView)h.itemView).setText(String.valueOf(years.get(p)));
            h.itemView.setOnClickListener(v -> listener.onClick(years.get(p)));
        }
        @Override public int getItemCount() { return years.size(); }
    }
}
