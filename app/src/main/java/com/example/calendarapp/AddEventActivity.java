package com.example.calendarapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendarapp.databinding.ActivityAddEventBinding;

import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    private ActivityAddEventBinding binding;
    private EventViewModel eventViewModel;
    private String selectedDate;
    private String selectedTime = "19:30";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Light status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        selectedDate = getIntent().getStringExtra("date");

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.timeBtn.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, h, m) -> {
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                binding.timeBtn.setText("Time: " + selectedTime);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        binding.saveBtn.setOnClickListener(v -> {
            String title = binding.titleInput.getText().toString().trim();
            if (title.isEmpty()) {
                binding.titleInput.setError("Title is required");
                return;
            }

            EventModel event = new EventModel(selectedDate, selectedTime, title);
            eventViewModel.insert(event);
            
            checkAndScheduleReminder(selectedDate, selectedTime, title);
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void checkAndScheduleReminder(String date, String time, String title) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Please grant permission to set reminders", Toast.LENGTH_LONG).show();
                return;
            }
        }
        scheduleReminder(date, time, title);
    }

    private void scheduleReminder(String date, String time, String title) {
        try {
            String[] d = date.split("-");
            String[] t = time.split(":");

            Calendar cal = Calendar.getInstance();
            cal.set(
                    Integer.parseInt(d[0]),
                    Integer.parseInt(d[1]) - 1,
                    Integer.parseInt(d[2]),
                    Integer.parseInt(t[0]),
                    Integer.parseInt(t[1])
            );
            cal.add(Calendar.MINUTE, -10);

            if (cal.getTimeInMillis() < System.currentTimeMillis()) {
                return;
            }

            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("title", title);
            intent.putExtra("date", date);
            intent.putExtra("time", time);

            PendingIntent pi = PendingIntent.getBroadcast(
                    this,
                    (int) System.currentTimeMillis(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
