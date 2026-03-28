package com.example.calendarapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface EventDao {

    @Insert
    void insert(EventModel event);

    @Update
    void update(EventModel event);

    @Delete
    void delete(EventModel event);

    @Query("SELECT * FROM events WHERE date = :date ORDER BY priority DESC, time ASC")
    LiveData<List<EventModel>> getEventsByDate(String date);

    @Query("SELECT * FROM events WHERE date LIKE :month || '%' ORDER BY date, priority DESC, time ASC")
    LiveData<List<EventModel>> getEventsByMonth(String month);
    
    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    EventModel getEventById(int id);
}
