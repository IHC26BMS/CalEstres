package com.example.calestres;

import android.content.Context;
import android.provider.Settings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

/** Uses the device's current time-zone for calendar and report dates. */
public final class AppDateTime {
    private AppDateTime() {
    }

    public static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    public static ZoneId zone(Context context) {
        String configuredZone = Settings.System.getString(
                context.getContentResolver(), "timezone");
        if (configuredZone != null && !configuredZone.isEmpty()) {
            try {
                return ZoneId.of(configuredZone);
            } catch (RuntimeException ignored) {
                // Fall back to the system zone if the setting is invalid.
            }
        }
        return ZoneId.systemDefault();
    }

    public static LocalDate today() {
        return LocalDate.now(zone());
    }

    public static LocalDate today(Context context) {
        return LocalDate.now(zone(context));
    }

    public static YearMonth currentMonth() {
        return YearMonth.now(zone());
    }

    public static YearMonth currentMonth(Context context) {
        return YearMonth.now(zone(context));
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(zone());
    }

    public static LocalDateTime now(Context context) {
        return LocalDateTime.now(zone(context));
    }
}
