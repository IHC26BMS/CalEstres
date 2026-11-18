package com.example.calestres;

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

    public static LocalDate today() {
        return LocalDate.now(zone());
    }

    public static YearMonth currentMonth() {
        return YearMonth.now(zone());
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(zone());
    }
}
