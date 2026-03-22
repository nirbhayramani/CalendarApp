package com.example.calendarapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "calendar.db";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE events (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "date TEXT NOT NULL," +
                        "time TEXT NOT NULL," +
                        "title TEXT NOT NULL)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS events");
        onCreate(db);
    }

    // CREATE
    public void addEvent(String date, String time, String title) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("date", date);
        cv.put("time", time);
        cv.put("title", title);
        db.insert("events", null, cv);
    }

    // READ (Day)
    public Cursor getEventsByDate(String date) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM events WHERE date=? ORDER BY time",
                new String[]{date}
        );
    }

    // READ (Month)
    public Cursor getEventsByMonth(String month) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM events WHERE date LIKE ? ORDER BY date, time",
                new String[]{month + "%"}
        );
    }

    // UPDATE
    public void updateEvent(int id, String time, String title) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("time", time);
        cv.put("title", title);
        db.update("events", cv, "id=?", new String[]{String.valueOf(id)});
    }

    // DELETE
    public void deleteEvent(int id) {
        getWritableDatabase().delete("events", "id=?", new String[]{String.valueOf(id)});
    }
}
