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

public class EditEventActivity extends AppCompatActivity {

    private ActivityAddEventBinding binding;
    private EventViewModel eventViewModel;
    private int id;
    private String date;
    private String time;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        binding = ActivityAddEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Light status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);

        id = getIntent().getIntExtra("id", -1);
        date = getIntent().getStringExtra("date");
        time = getIntent().getStringExtra("time");
        String title = getIntent().getStringExtra("title");

        binding.toolbar.setTitle("Edit Event");
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.titleInput.setText(title);
        binding.timeBtn.setText("Time: " + time);
        binding.saveBtn.setText("Update Event");

        binding.timeBtn.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, h, m) -> {
                time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                binding.timeBtn.setText("Time: " + time);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        binding.saveBtn.setOnClickListener(v -> {
            String updatedTitle = binding.titleInput.getText().toString().trim();
            if (updatedTitle.isEmpty()) {
                binding.titleInput.setError("Title is required");
                return;
            }

            EventModel event = new EventModel(id, date, time, updatedTitle);
            eventViewModel.update(event);

            checkAndScheduleReminder(date, time, updatedTitle);
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
                    id,
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
