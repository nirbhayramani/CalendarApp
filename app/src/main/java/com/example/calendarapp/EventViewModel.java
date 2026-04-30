package com.example.calendarapp;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class EventViewModel extends AndroidViewModel {

    private final EventRepository repository;

    public EventViewModel(@NonNull Application application) {
        super(application);
        repository = new EventRepository(application);
    }

    public LiveData<List<EventModel>> getEventsByDate(String date) {
        return repository.getEventsByDate(date);
    }

    public LiveData<List<EventModel>> getEventsByMonth(String month) {
        return repository.getEventsByMonth(month);
    }

    public void insert(EventModel event, EventRepository.OnEventInsertedCallback callback) {
        repository.insert(event, callback);
    }

    public void update(EventModel event) {
        repository.update(event);
    }

    public void delete(EventModel event) {
        repository.delete(event);
    }

    public List<EventModel> getEventsByDateSync(String date) {
        return repository.getEventsByDateSync(date);
    }

    /**
     * FIX 3 - Batch event dot queries
     * Added synchronous ViewModel method to fetch all events for a month.
     * This is utilized by the CalendarAdapter to fetch month data in a single background task.
     */
    public List<EventModel> getEventsByMonthSync(String month) {
        return repository.getEventsByMonthSync(month);
    }
}
