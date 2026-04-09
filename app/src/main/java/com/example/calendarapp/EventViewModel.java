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
}
