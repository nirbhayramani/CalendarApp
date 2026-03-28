package com.example.calendarapp;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventModel {

    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String date;
    public String time;
    public String title;
    
    // Priority: 0 = Low, 1 = Medium, 2 = High
    public int priority;
    
    // Reminder Type: 0 = Notification, 1 = Alarm
    public int reminderType;

    public EventModel(int id, String date, String time, String title, int priority, int reminderType) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.title = title;
        this.priority = priority;
        this.reminderType = reminderType;
    }

    @Ignore
    public EventModel(String date, String time, String title, int priority, int reminderType) {
        this.date = date;
        this.time = time;
        this.title = title;
        this.priority = priority;
        this.reminderType = reminderType;
    }
}
