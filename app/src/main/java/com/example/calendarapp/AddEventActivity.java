package com.example.calendarapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendarapp.databinding.ActivityAddEventBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    private ActivityAddEventBinding binding;
    private EventViewModel eventViewModel;
    private String selectedDate;
    private String selectedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        selectedDate = getIntent().getStringExtra("date");

        setupDateDisplay();
        setupDefaultTime();

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        binding.reminderGroup.check(R.id.btnNotification);

        binding.timeBtn.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            String[] t = selectedTime.split(":");
            new TimePickerDialog(this, (view, h, m) -> {
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                binding.timeBtn.setText("Time: " + selectedTime);
            }, Integer.parseInt(t[0]), Integer.parseInt(t[1]), false).show();
        });

        binding.saveBtn.setOnClickListener(v -> {
            String title = binding.titleInput.getText().toString().trim();
            if (title.isEmpty()) {
                binding.titleInput.setError("Title is required");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 102);
                    return;
                }
            }

            saveAndSchedule(title);
        });
    }

    private void setupDateDisplay() {
        if (selectedDate != null) {
            try {
                String[] d = selectedDate.split("-");
                Calendar cal = Calendar.getInstance();
                cal.set(Integer.parseInt(d[0]), Integer.parseInt(d[1]) - 1, Integer.parseInt(d[2]));
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault());
                binding.dateDisplay.setText("Adding event for: " + sdf.format(cal.getTime()));
                binding.dateDisplay.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                binding.dateDisplay.setVisibility(View.GONE);
            }
        }
    }

    private void setupDefaultTime() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 0);
        selectedTime = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), 0);
        binding.timeBtn.setText("Time: " + selectedTime);
    }

    private void saveAndSchedule(String title) {
        int priority = 0;
        if (binding.priorityGroup.getCheckedChipId() == R.id.chipMedium) priority = 1;
        else if (binding.priorityGroup.getCheckedChipId() == R.id.chipHigh) priority = 2;

        int reminderType = (binding.reminderGroup.getCheckedButtonId() == R.id.btnAlarm) ? 1 : 0;

        EventModel event = new EventModel(selectedDate, selectedTime, title, priority, reminderType);
        
        eventViewModel.insert(event, insertedEvent -> {
            runOnUiThread(() -> {
                checkAndScheduleReminder(insertedEvent);
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });
        });
    }

    private void checkAndScheduleReminder(EventModel event) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Please grant permission to set reminders", Toast.LENGTH_LONG).show();
                return;
            }
        }
        scheduleReminder(event);
    }

    private void scheduleReminder(EventModel event) {
        try {
            String[] d = event.date.split("-");
            String[] t = event.time.split(":");

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, Integer.parseInt(d[0]));
            cal.set(Calendar.MONTH, Integer.parseInt(d[1]) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(d[2]));
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(t[1]));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            if (cal.getTimeInMillis() <= System.currentTimeMillis()) return;

            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("title", event.title);
            intent.putExtra("date", event.date);
            intent.putExtra("time", event.time);
            intent.putExtra("reminderType", event.reminderType);

            PendingIntent pi = PendingIntent.getBroadcast(
                    this,
                    event.id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                Log.d("CalendarApp", "Alarm/Notification scheduled for: " + cal.getTime() + " with ID: " + event.id);
            }

        } catch (Exception e) {
            Log.e("CalendarApp", "Scheduling error", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            String title = binding.titleInput.getText().toString().trim();
            saveAndSchedule(title);
        } else {
            Toast.makeText(this, "Permission required for reminders", Toast.LENGTH_SHORT).show();
        }
    }
}
