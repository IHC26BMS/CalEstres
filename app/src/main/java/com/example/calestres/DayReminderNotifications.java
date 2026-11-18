package com.example.calestres;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

/** Schedules the local 10:00 reminder and requests notification permission when needed. */
public final class DayReminderNotifications {
    static final String ACTION_REMIND = "com.example.calestres.ACTION_DAILY_REMINDER";
    static final int REQUEST_NOTIFICATIONS = 701;
    private static final int REMINDER_REQUEST_CODE = 702;

    private DayReminderNotifications() { }

    public static void requestPermissionAndSchedule(Activity activity) {
        ReminderReceiver.createNotificationChannel(activity);
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            return;
        }
        scheduleNextReminder(activity);
    }

    public static void scheduleNextReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar next = Calendar.getInstance(
                java.util.TimeZone.getTimeZone(AppDateTime.zone(context)));
        next.set(Calendar.HOUR_OF_DAY, 10);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(Calendar.getInstance())) next.add(Calendar.DAY_OF_YEAR, 1);

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMIND);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REMINDER_REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
    }
}
