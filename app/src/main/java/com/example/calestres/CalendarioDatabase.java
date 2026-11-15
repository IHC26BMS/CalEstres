package com.example.calestres;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/** Local, normalized SQLite storage for daily check-ins. */
public class CalendarioDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "calestres.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_ENTRIES = "daily_entries";
    private static final String TABLE_METRICS = "daily_metrics";
    private static final String TABLE_NOTES = "daily_notes";
    private static final String COLUMN_DATE = "entry_date";

    public CalendarioDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase database) {
        super.onConfigure(database);
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE_ENTRIES + " ("
                + COLUMN_DATE + " TEXT PRIMARY KEY NOT NULL, "
                + "created_at INTEGER NOT NULL DEFAULT (strftime('%s','now'))) ");
        database.execSQL("CREATE TABLE " + TABLE_METRICS + " ("
                + COLUMN_DATE + " TEXT PRIMARY KEY NOT NULL, "
                + "stress INTEGER NOT NULL CHECK(stress BETWEEN 1 AND 10), "
                + "sleep_hours REAL NOT NULL CHECK(sleep_hours BETWEEN 0 AND 12), "
                + "meals INTEGER NOT NULL CHECK(meals BETWEEN 0 AND 10), "
                + "FOREIGN KEY(" + COLUMN_DATE + ") REFERENCES " + TABLE_ENTRIES
                + "(" + COLUMN_DATE + ") ON DELETE CASCADE)");
        database.execSQL("CREATE TABLE " + TABLE_NOTES + " ("
                + COLUMN_DATE + " TEXT PRIMARY KEY NOT NULL, "
                + "note_text TEXT NOT NULL DEFAULT '', "
                + "FOREIGN KEY(" + COLUMN_DATE + ") REFERENCES " + TABLE_ENTRIES
                + "(" + COLUMN_DATE + ") ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // Add forward-only schema migrations here when DATABASE_VERSION changes.
    }

    public Map<LocalDate, Entry> loadEntries() {
        Map<LocalDate, Entry> entries = new TreeMap<>();
        SQLiteDatabase database = getReadableDatabase();
        String query = "SELECT e." + COLUMN_DATE + ", m.stress, m.sleep_hours, m.meals, n.note_text "
                + "FROM " + TABLE_ENTRIES + " e "
                + "INNER JOIN " + TABLE_METRICS + " m ON e." + COLUMN_DATE + " = m." + COLUMN_DATE + " "
                + "LEFT JOIN " + TABLE_NOTES + " n ON e." + COLUMN_DATE + " = n." + COLUMN_DATE + " "
                + "ORDER BY e." + COLUMN_DATE;
        try (Cursor cursor = database.rawQuery(query, null)) {
            int dateColumn = cursor.getColumnIndexOrThrow(COLUMN_DATE);
            int stressColumn = cursor.getColumnIndexOrThrow("stress");
            int sleepColumn = cursor.getColumnIndexOrThrow("sleep_hours");
            int mealsColumn = cursor.getColumnIndexOrThrow("meals");
            int noteColumn = cursor.getColumnIndexOrThrow("note_text");
            while (cursor.moveToNext()) {
                LocalDate date = LocalDate.parse(cursor.getString(dateColumn));
                Entry entry = new Entry();
                entry.stress = cursor.getInt(stressColumn);
                entry.sleepHours = cursor.getDouble(sleepColumn);
                entry.meals = cursor.getInt(mealsColumn);
                entry.notes = cursor.isNull(noteColumn) ? "" : cursor.getString(noteColumn);
                entries.put(date, entry);
            }
        }
        return entries;
    }

    public void saveEntry(LocalDate date, int stress, double sleepHours, int meals, String notes) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            upsertEntry(database, date.toString(), stress, sleepHours, meals, notes, false);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void saveNote(LocalDate date, String note) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_DATE, date.toString());
            values.put("note_text", note == null ? "" : note);
            database.insertWithOnConflict(TABLE_NOTES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    /** Imports the former JSON preference store once, before it is cleared by the activity. */
    public boolean migrateLegacyJson(String json) {
        final JSONObject legacy;
        try {
            legacy = new JSONObject(json);
        } catch (JSONException | RuntimeException exception) {
            return false;
        }

        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            java.util.Iterator<String> dates = legacy.keys();
            while (dates.hasNext()) {
                String dateKey = dates.next();
                JSONObject item = legacy.optJSONObject(dateKey);
                if (item == null) continue;
                try {
                    LocalDate.parse(dateKey);
                } catch (RuntimeException invalidDate) {
                    continue;
                }
                int stress = clamp(item.optInt("stress", 5), 1, 10);
                double sleepHours = Math.max(0, Math.min(12, item.optDouble("sleepHours", 7.0)));
                int meals = clamp(item.optInt("meals", 3), 0, 10);
                String notes = item.optString("notes", "");
                upsertEntry(database, dateKey, stress, sleepHours, meals, notes, true);
            }
            database.setTransactionSuccessful();
            return true;
        } catch (RuntimeException exception) {
            return false;
        } finally {
            database.endTransaction();
        }
    }

    private void upsertEntry(SQLiteDatabase database, String date, int stress, double sleepHours,
                             int meals, String notes, boolean preserveExisting) {
        ContentValues entryValues = new ContentValues();
        entryValues.put(COLUMN_DATE, date);
        long rowId = database.insertWithOnConflict(TABLE_ENTRIES, null, entryValues,
            SQLiteDatabase.CONFLICT_IGNORE);
        if (rowId == -1 && preserveExisting) return;

        ContentValues metrics = new ContentValues();
        metrics.put(COLUMN_DATE, date);
        metrics.put("stress", clamp(stress, 1, 10));
        metrics.put("sleep_hours", Math.max(0, Math.min(12, sleepHours)));
        metrics.put("meals", clamp(meals, 0, 10));
        database.insertWithOnConflict(TABLE_METRICS, null, metrics, SQLiteDatabase.CONFLICT_REPLACE);

        ContentValues noteValues = new ContentValues();
        noteValues.put(COLUMN_DATE, date);
        noteValues.put("note_text", notes == null ? "" : notes);
        database.insertWithOnConflict(TABLE_NOTES, null, noteValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static class Entry {
        public int stress = 5;
        public double sleepHours = 7.0;
        public int meals = 3;
        public String notes = "";
    }
}
