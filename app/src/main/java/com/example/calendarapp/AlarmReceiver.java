package com.example.calendarapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID_NOTIFICATION = "EVENT_NOTIFICATION_CHANNEL";
    private static final String CHANNEL_ID_ALARM = "EVENT_ALARM_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");
        int type = intent.getIntExtra("reminderType", 0); // 0 = Notification, 1 = Alarm

        if (type == 1) {
            // FIRE THE FLASH SCREEN ALARM
            Intent alarmIntent = new Intent(context, AlarmActivity.class);
            alarmIntent.putExtra("title", title);
            alarmIntent.putExtra("time", time);
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(alarmIntent);
        }

        // Always show a notification as a fallback
        showNotification(context, title, date, time, type);
    }

    private void showNotification(Context context, String title, String date, String time, int type) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notifyChannel = new NotificationChannel(
                    CHANNEL_ID_NOTIFICATION,
                    "Event Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(notifyChannel);

            NotificationChannel alarmChannel = new NotificationChannel(
                    CHANNEL_ID_ALARM,
                    "Critical Event Alarms",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(alarmChannel);
        }

        String channelId = (type == 1) ? CHANNEL_ID_ALARM : CHANNEL_ID_NOTIFICATION;
        Uri soundUri = RingtoneManager.getDefaultUri(type == 1 ? RingtoneManager.TYPE_ALARM : RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(type == 1 ? "ALARM: " + title : "Reminder: " + title)
                .setContentText(date + " at " + time)
                .setPriority(type == 1 ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setSound(soundUri)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
