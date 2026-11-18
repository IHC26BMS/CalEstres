package com.example.calestres;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.time.LocalDate;

/** Delivers the reminder, then schedules the next daily check. */
public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "daily_checkin_reminders";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            DayReminderNotifications.scheduleNextReminder(context);
            return;
        }

        LocalDate yesterday = AppDateTime.today(context).minusDays(1);
        CalendarioDatabase database = new CalendarioDatabase(context);
        boolean alreadyLogged;
        try {
            alreadyLogged = database.hasEntry(yesterday);
        } finally {
            database.close();
        }

        if (!alreadyLogged && notificationsAllowed(context)) {
            showReminder(context);
        }
        DayReminderNotifications.scheduleNextReminder(context);
    }

    static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Recordatorios diarios", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Avisos para completar el registro diario de bienestar");
        manager.createNotificationChannel(channel);
    }

    private boolean notificationsAllowed(Context context) {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showReminder(Context context) {
        createNotificationChannel(context);
        Intent openCalendar = new Intent(context, Calendario.class);
        openCalendar.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, openCalendar,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Tu registro diario te espera")
                .setContentText("Tómate un momento para registrar cómo fue tu día de ayer.")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        "Tómate un momento para registrar tu nivel de estrés, sueño, comidas y notas de ayer."))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification.build());
        } catch (SecurityException ignored) {
            // Notification permission may have been revoked after the alarm was scheduled.
        }
    }
}
