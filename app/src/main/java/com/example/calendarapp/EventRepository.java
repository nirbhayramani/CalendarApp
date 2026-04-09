package com.example.calendarapp;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventRepository {

    private final EventDao eventDao;
    private final ExecutorService executorService;
    private final Application application;

    public EventRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        eventDao = db.eventDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<EventModel>> getEventsByDate(String date) {
        return eventDao.getEventsByDate(date);
    }

    public LiveData<List<EventModel>> getEventsByMonth(String month) {
        return eventDao.getEventsByMonth(month);
    }

    public void insert(EventModel event, OnEventInsertedCallback callback) {
        executorService.execute(() -> {
            long id = eventDao.insert(event);
            event.id = (int) id;
            if (callback != null) {
                callback.onInserted(event);
            }
        });
    }

    public void update(EventModel event) {
        executorService.execute(() -> eventDao.update(event));
    }

    public void delete(EventModel event) {
        executorService.execute(() -> {
            cancelAlarm(event.id);
            eventDao.delete(event);
        });
    }

    private void cancelAlarm(int id) {
        Intent intent = new Intent(application, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                application,
                id,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pi != null) {
            AlarmManager alarmManager = (AlarmManager) application.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pi);
            }
            pi.cancel();
        }
    }

    public interface OnEventInsertedCallback {
        void onInserted(EventModel event);
    }

    public List<EventModel> getEventsByDateSync(String date) {
        return eventDao.getEventsByDateSync(date);
    }
}
