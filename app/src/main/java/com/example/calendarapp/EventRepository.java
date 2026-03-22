package com.example.calendarapp;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventRepository {

    private final EventDao eventDao;
    private final ExecutorService executorService;

    public EventRepository(Application application) {
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

    public void insert(EventModel event) {
        executorService.execute(() -> eventDao.insert(event));
    }

    public void update(EventModel event) {
        executorService.execute(() -> eventDao.update(event));
    }

    public void delete(EventModel event) {
        executorService.execute(() -> eventDao.delete(event));
    }
}
