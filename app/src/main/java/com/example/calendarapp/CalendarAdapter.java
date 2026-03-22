package com.example.calendarapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.MonthViewHolder> {

    private final Context context;
    private final Calendar baseCalendar;
    private final OnDateSelectedListener listener;
    private static final int START_POSITION = 500;
    private Calendar selectedDate;

    public interface OnDateSelectedListener {
        void onDateSelected(Calendar calendar, boolean isCurrentMonth);
    }

    public CalendarAdapter(Context context, Calendar selectedDate, OnDateSelectedListener listener) {
        this.context = context;
        this.baseCalendar = (Calendar) Calendar.getInstance().clone();
        this.baseCalendar.set(Calendar.DAY_OF_MONTH, 1);
        this.selectedDate = (Calendar) selectedDate.clone();
        this.listener = listener;
    }

    public void setSelectedDate(Calendar date) {
        this.selectedDate = (Calendar) date.clone();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_month, parent, false);
        return new MonthViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        Calendar monthCal = (Calendar) baseCalendar.clone();
        monthCal.add(Calendar.MONTH, position - START_POSITION);
        holder.bind(monthCal);
    }

    @Override
    public int getItemCount() {
        return 1000;
    }

    class MonthViewHolder extends RecyclerView.ViewHolder {
        RecyclerView monthGrid;

        public MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            monthGrid = itemView.findViewById(R.id.monthGrid);
        }

        void bind(Calendar monthCal) {
            List<Calendar> days = new ArrayList<>();
            Calendar temp = (Calendar) monthCal.clone();
            temp.set(Calendar.DAY_OF_MONTH, 1);
            
            int firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK) - 1;
            temp.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek);

            for (int i = 0; i < 42; i++) {
                days.add((Calendar) temp.clone());
                temp.add(Calendar.DAY_OF_MONTH, 1);
            }

            DayAdapter adapter = new DayAdapter(context, days, monthCal.get(Calendar.MONTH), selectedDate, (date) -> {
                boolean isCurrentMonth = date.get(Calendar.MONTH) == monthCal.get(Calendar.MONTH);
                selectedDate = (Calendar) date.clone();
                listener.onDateSelected(selectedDate, isCurrentMonth);
                notifyDataSetChanged();
            });
            monthGrid.setAdapter(adapter);
        }
    }

    static class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {
        private final Context context;
        private final List<Calendar> days;
        private final int currentMonth;
        private final Calendar selectedDate;
        private final OnDayClickListener listener;

        interface OnDayClickListener {
            void onDayClick(Calendar date);
        }

        DayAdapter(Context context, List<Calendar> days, int currentMonth, Calendar selectedDate, OnDayClickListener listener) {
            this.context = context;
            this.days = days;
            this.currentMonth = currentMonth;
            this.selectedDate = selectedDate;
            this.listener = listener;
        }

        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            Calendar day = days.get(position);
            holder.dayText.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));

            boolean isCurrentMonth = day.get(Calendar.MONTH) == currentMonth;
            holder.dayText.setAlpha(isCurrentMonth ? 1.0f : 0.3f);

            boolean isSelected = isSameDay(day, selectedDate);
            holder.selectionBubble.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
            holder.dayText.setTextColor(isSelected ? context.getColor(R.color.white) : context.getColor(R.color.text_main));

            holder.itemView.setOnClickListener(v -> listener.onDayClick(day));
        }

        @Override
        public int getItemCount() { return days.size(); }

        private boolean isSameDay(Calendar c1, Calendar c2) {
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                   c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
        }

        class DayViewHolder extends RecyclerView.ViewHolder {
            TextView dayText;
            View selectionBubble;
            DayViewHolder(View v) {
                super(v);
                dayText = v.findViewById(R.id.dayText);
                selectionBubble = v.findViewById(R.id.selectionBubble);
            }
        }
    }
}
