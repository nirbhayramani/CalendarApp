package com.example.calendarapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.calendarapp.databinding.ActivityMainBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * MainActivity — The main screen that holds the Calendar and Agenda.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Calendar selectedCalendar;
    private EventViewModel eventViewModel;
    private EventAdapter agendaAdapter;
    private CalendarAdapter calendarAdapter;

    // Flags to track if the user is actively swiping the calendar
    private boolean isUserScrolling = false;
    private boolean isProgrammaticScroll = false;

    private LiveData<List<EventModel>> currentAgendaLiveData;
    private Observer<List<EventModel>> agendaObserver;

    // Handles returning from Add/Edit screens
    private final ActivityResultLauncher<Intent> eventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String date = result.getData().getStringExtra("date");
                    if (date != null) {
                        try {
                            String[] d = date.split("-");
                            selectedCalendar = Calendar.getInstance();
                            selectedCalendar.set(
                                    Integer.parseInt(d[0]),
                                    Integer.parseInt(d[1]) - 1,
                                    Integer.parseInt(d[2])
                            );
                            navigateToMonth(selectedCalendar, false);
                            updateUI(selectedCalendar);
                        } catch (Exception e) {
                            Log.e("CalendarApp", "Error parsing returned date", e);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Make status bar icons dark
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        selectedCalendar = Calendar.getInstance();

        buildAgendaObserver();
        setupFixedHeader();
        setupCustomCalendar();
        setupAgendaList();
        setupMotionSync(); // FIX: Sync logic added here

        updateUI(selectedCalendar);

        // UI Click Listeners
        binding.addEventBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            intent.putExtra("date", getFormattedDate(selectedCalendar));
            eventLauncher.launch(intent);
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

        checkNotificationPermission();
    }

    /**
     * BEGINNER-FRIENDLY FIX: The "Sync Safety Net"
     * WHY: When you swipe the agenda, the calendar changes size. This makes ViewPager2
     *      glitch and jump back to Jan 1900 (the very start of the list).
     * HOW: We watch the "Motion" (the swipe). If the calendar jumps while you are
     *      swiping, we instantly snap it back to the month you actually had selected.
     */
    private void setupMotionSync() {
        binding.motionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {}

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
                // If the calendar is NOT being swiped by a finger, ensure it stays on the right month
                int expectedPos = (selectedCalendar.get(Calendar.YEAR) - CalendarAdapter.START_YEAR) * 12 
                                  + selectedCalendar.get(Calendar.MONTH);
                
                if (binding.calendarViewPager.getCurrentItem() != expectedPos && !isUserScrolling) {
                    navigateToMonth(selectedCalendar, false);
                }
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                // Final check when the swipe animation stops
                navigateToMonth(selectedCalendar, false);
                updateUI(selectedCalendar);
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {}
        });
    }

    private void setupCustomCalendar() {
        calendarAdapter = new CalendarAdapter(
                this,
                selectedCalendar,
                eventViewModel,
                (calendar, isCurrentMonth) -> {
                    selectedCalendar = (Calendar) calendar.clone();
                    if (!isCurrentMonth) {
                        navigateToMonth(selectedCalendar, false);
                    }
                    updateUI(selectedCalendar);
                });

        binding.calendarViewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        binding.calendarViewPager.setAdapter(calendarAdapter);

        // Remove the blue/glow effect at the top/bottom
        binding.calendarViewPager.post(() -> {
            RecyclerView vp2RecyclerView = (RecyclerView) binding.calendarViewPager.getChildAt(0);
            if (vp2RecyclerView != null) {
                vp2RecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            }
        });

        navigateToMonth(selectedCalendar, false);

        binding.calendarViewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageScrollStateChanged(int state) {
                        super.onPageScrollStateChanged(state);
                        // Is the user actually touching the calendar pages?
                        if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                            isProgrammaticScroll = false;
                            isUserScrolling = true;
                        } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                            isUserScrolling = false;
                            isProgrammaticScroll = false;
                        }
                    }

                    @Override
                    public void onPageSelected(int position) {
                        // Only update our "selected month" if the user swiped the calendar manually
                        if (isUserScrolling && !isProgrammaticScroll) {
                            int year  = CalendarAdapter.START_YEAR + (position / 12);
                            int month = position % 12;

                            Calendar headerCal = Calendar.getInstance();
                            headerCal.set(year, month, 1);
                            updateCalendarHeaderText(headerCal);

                            selectedCalendar.set(year, month, 1);
                            calendarAdapter.setSelectedDate(selectedCalendar);
                        }
                    }
                });
    }

    private void buildAgendaObserver() {
        agendaObserver = events -> {
            if (events != null && !events.isEmpty()) {
                agendaAdapter.updateData(events);
                binding.emptyAgendaLayout.setVisibility(View.GONE);
                binding.agendaList.setVisibility(View.VISIBLE);
            } else {
                agendaAdapter.updateData(new ArrayList<>());
                binding.emptyAgendaLayout.setVisibility(View.VISIBLE);
                binding.agendaList.setVisibility(View.GONE);
            }
        };
    }

    private void setupFixedHeader() {
        Calendar today = Calendar.getInstance();
        binding.todayDateText.setText(
                new SimpleDateFormat("dd MMMM", Locale.getDefault()).format(today.getTime()));

        binding.todayHeader.setOnClickListener(v -> {
            selectedCalendar = Calendar.getInstance();
            navigateToMonth(selectedCalendar, false);
            updateUI(selectedCalendar);
        });
    }

    private void setupAgendaList() {
        agendaAdapter = new EventAdapter(this, new ArrayList<>(),
                new EventAdapter.OnEventActionListener() {
                    @Override public void onDelete(EventModel event) { eventViewModel.delete(event); }
                    @Override public void onEdit(EventModel event) {
                        Intent i = new Intent(MainActivity.this, EditEventActivity.class);
                        i.putExtra("id", event.id);
                        i.putExtra("date", event.date);
                        i.putExtra("time", event.time);
                        i.putExtra("title", event.title);
                        i.putExtra("priority", event.priority);
                        i.putExtra("reminderType", event.reminderType);
                        eventLauncher.launch(i);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    }
                });
        binding.agendaList.setAdapter(agendaAdapter);
    }

    private void updateUI(Calendar calendar) {
        updateCalendarHeaderText(calendar);
        calendarAdapter.setSelectedDate(calendar);

        String dateKey = getFormattedDate(calendar);
        LiveData<List<EventModel>> newLiveData = eventViewModel.getEventsByDate(dateKey);

        if (currentAgendaLiveData != null) {
            currentAgendaLiveData.removeObserver(agendaObserver);
        }

        currentAgendaLiveData = newLiveData;
        currentAgendaLiveData.observe(this, agendaObserver);
    }

    private void updateCalendarHeaderText(Calendar calendar) {
        binding.currentMonthText.setText(new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime()));
        binding.currentYearText.setText(String.valueOf(calendar.get(Calendar.YEAR)));
    }

    private void navigateToMonth(Calendar target, boolean smoothScroll) {
        int position = (target.get(Calendar.YEAR) - CalendarAdapter.START_YEAR) * 12 + target.get(Calendar.MONTH);
        if (position >= 0 && position < CalendarAdapter.TOTAL_MONTHS) {
            isProgrammaticScroll = true;
            binding.calendarViewPager.setCurrentItem(position, smoothScroll);
        }
    }

    private void showMonthSelector() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_month_selector, null);
        RecyclerView monthList = view.findViewById(R.id.monthList);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(view).create();
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        monthList.setLayoutManager(new LinearLayoutManager(this));
        monthList.setAdapter(new MonthListAdapter(months, selectedCalendar.get(Calendar.MONTH), position -> {
            Calendar target = (Calendar) selectedCalendar.clone();
            target.set(Calendar.MONTH, position);
            target.set(Calendar.DAY_OF_MONTH, 1);
            selectedCalendar = target;
            navigateToMonth(selectedCalendar, false);
            updateUI(selectedCalendar);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void showYearSelector() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_year_selector, null);
        TextView yearDisplay = view.findViewById(R.id.selectedYearDisplay);
        EditText yearInput = view.findViewById(R.id.yearInput);
        RecyclerView yearListView = view.findViewById(R.id.yearList);
        yearDisplay.setText(String.valueOf(selectedCalendar.get(Calendar.YEAR)));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(view).create();
        yearDisplay.setOnClickListener(v -> {
            yearDisplay.setVisibility(View.GONE); yearInput.setVisibility(View.VISIBLE);
            yearInput.setText(yearDisplay.getText()); yearInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(yearInput, InputMethodManager.SHOW_IMPLICIT);
        });
        yearInput.setOnEditorActionListener((v, actionId, event) -> {
            String input = yearInput.getText().toString().trim();
            if (input.length() == 4) {
                try {
                    int year = Integer.parseInt(input);
                    while (year > CalendarAdapter.END_YEAR) year -= 400;
                    while (year < CalendarAdapter.START_YEAR) year += 400;
                    Calendar target = (Calendar) selectedCalendar.clone();
                    target.set(Calendar.YEAR, year); target.set(Calendar.DAY_OF_MONTH, 1);
                    selectedCalendar = target; navigateToMonth(selectedCalendar, false);
                    updateUI(selectedCalendar); dialog.dismiss(); return true;
                } catch (NumberFormatException ignored) {}
            }
            return false;
        });
        List<Integer> years = new ArrayList<>();
        for (int i = CalendarAdapter.START_YEAR; i <= CalendarAdapter.END_YEAR; i++) years.add(i);
        yearListView.setLayoutManager(new LinearLayoutManager(this));
        yearListView.setAdapter(new YearListAdapter(years, selectedCalendar.get(Calendar.YEAR), year -> {
            Calendar target = (Calendar) selectedCalendar.clone();
            target.set(Calendar.YEAR, year); target.set(Calendar.DAY_OF_MONTH, 1);
            selectedCalendar = target; navigateToMonth(selectedCalendar, false);
            updateUI(selectedCalendar); dialog.dismiss();
        }));
        yearListView.scrollToPosition(selectedCalendar.get(Calendar.YEAR) - CalendarAdapter.START_YEAR);
        dialog.show();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private String getFormattedDate(Calendar calendar) { return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime()); }
    private String getMonthKey(Calendar calendar) { return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.getTime()); }

    private static class MonthListAdapter extends RecyclerView.Adapter<MonthListAdapter.ViewHolder> {
        String[] months; int selectedMonth; OnItemClickListener listener;
        interface OnItemClickListener { void onClick(int pos); }
        MonthListAdapter(String[] m, int selected, OnItemClickListener l) { months = m; selectedMonth = selected; listener = l; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_1, p, false);
            return new ViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            TextView tv = (TextView) h.itemView; tv.setText(months[p]);
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), p == selectedMonth ? R.color.accent : R.color.text_main));
            tv.setTypeface(null, p == selectedMonth ? Typeface.BOLD : Typeface.NORMAL);
            h.itemView.setOnClickListener(v -> listener.onClick(p));
        }
        @Override public int getItemCount() { return 12; }
        static class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(View v) { super(v); } }
    }

    private static class YearListAdapter extends RecyclerView.Adapter<YearListAdapter.ViewHolder> {
        List<Integer> years; int selectedYear; OnYearClickListener listener;
        interface OnYearClickListener { void onClick(int year); }
        YearListAdapter(List<Integer> y, int selected, OnYearClickListener l) { years = y; selectedYear = selected; listener = l; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_1, p, false);
            return new ViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            TextView tv = (TextView) h.itemView; int year = years.get(p); tv.setText(String.valueOf(year));
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), year == selectedYear ? R.color.accent : R.color.text_main));
            tv.setTypeface(null, year == selectedYear ? Typeface.BOLD : Typeface.NORMAL);
            h.itemView.setOnClickListener(v -> listener.onClick(year));
        }
        @Override public int getItemCount() { return years.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder { ViewHolder(View v) { super(v); } }
    }
}
