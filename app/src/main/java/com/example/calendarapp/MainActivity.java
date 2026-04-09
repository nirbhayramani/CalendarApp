package com.example.calendarapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.calendarapp.databinding.ActivityMainBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Calendar selectedCalendar;
    private EventViewModel eventViewModel;
    private EventAdapter agendaAdapter;
    private CalendarAdapter calendarAdapter;
    private final int START_POSITION = CalendarAdapter.START_POSITION;
    private boolean isUserScrolling = false;

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

        binding.monthHeader.setOnClickListener(v -> showMonthYearSelector());
        
        binding.viewAllBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MonthEventsActivity.class);
            intent.putExtra("month", getMonthKey(selectedCalendar));
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
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
        calendarAdapter = new CalendarAdapter(this, selectedCalendar, eventViewModel, (calendar, isCurrentMonth) -> {
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
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                isUserScrolling = (state != ViewPager2.SCROLL_STATE_IDLE);
            }

            @Override
            public void onPageSelected(int position) {
                if (isUserScrolling) {
                    Calendar monthCal = Calendar.getInstance();
                    monthCal.set(Calendar.DAY_OF_MONTH, 1);
                    monthCal.add(Calendar.MONTH, position - START_POSITION);
                    updateCalendarHeaderText(monthCal);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (calendarAdapter != null) {
            calendarAdapter.notifyDataSetChanged();
        }
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

    private void showMonthYearSelector() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_month_year_picker, null);
        TextView yearText = view.findViewById(R.id.selectedYearText);
        View prevYear = view.findViewById(R.id.prevYear);
        View nextYear = view.findViewById(R.id.nextYear);
        RecyclerView monthGrid = view.findViewById(R.id.monthGrid);

        final int[] tempYear = {selectedCalendar.get(Calendar.YEAR)};
        yearText.setText(String.valueOf(tempYear[0]));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Select Month & Year")
                .setView(view)
                .create();

        prevYear.setOnClickListener(v -> {
            tempYear[0]--;
            yearText.setText(String.valueOf(tempYear[0]));
        });

        nextYear.setOnClickListener(v -> {
            tempYear[0]++;
            yearText.setText(String.valueOf(tempYear[0]));
        });

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        
        MonthGridAdapter adapter = new MonthGridAdapter(months, selectedCalendar.get(Calendar.MONTH), position -> {
            selectedCalendar.set(Calendar.YEAR, tempYear[0]);
            selectedCalendar.set(Calendar.MONTH, position);
            navigateToMonth(selectedCalendar);
            updateUI(selectedCalendar);
            dialog.dismiss();
        });
        monthGrid.setAdapter(adapter);
        dialog.show();
    }

    private String getFormattedDate(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
    }

    private String getMonthKey(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.getTime());
    }

    private static class MonthGridAdapter extends RecyclerView.Adapter<MonthGridAdapter.ViewHolder> {
        String[] months;
        int selectedMonth;
        OnItemClickListener listener;
        interface OnItemClickListener { void onClick(int pos); }
        
        MonthGridAdapter(String[] m, int selected, OnItemClickListener l) { 
            months = m; 
            selectedMonth = selected;
            listener = l; 
        }
        
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            TextView tv = new TextView(p.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(-1, 120));
            tv.setGravity(17);
            tv.setTextSize(16);
            return new ViewHolder(tv);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            TextView tv = (TextView) h.itemView;
            tv.setText(months[p]);
            if (p == selectedMonth) {
                tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.accent));
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.text_main));
                tv.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
            h.itemView.setOnClickListener(v -> listener.onClick(p));
        }
        @Override public int getItemCount() { return 12; }
        static class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(View v) { super(v); } }
    }
}
