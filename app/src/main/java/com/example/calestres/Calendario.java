package com.example.calestres;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class Calendario extends AppCompatActivity {
    private static final String PREFERENCES = "cal_estres_entries";
    private static final String ENTRY_KEY = "entries";
    private static final Locale SPANISH = new Locale("es", "ES");
    private static final int CYAN = Color.rgb(32, 167, 184);
    private static final int DARK = Color.rgb(23, 59, 70);
    private static final int MUTED = Color.rgb(113, 133, 140);
    private static final int GREEN = Color.rgb(42, 159, 99);
    private static final int YELLOW = Color.rgb(207, 145, 22);
    private static final int RED = Color.rgb(207, 77, 77);

    private final Map<LocalDate, DayEntry> entries = new TreeMap<>();
    private CalendarioDatabase database;
    private LocalDate selectedDate;
    private YearMonth visibleMonth;
    private LocalDate nextDayToLog;
    private boolean changingSpinners;
    private int selectedMeals = 3;

    private Spinner monthSpinner;
    private Spinner yearSpinner;
    private LinearLayout weekdaysRow;
    private LinearLayout calendarGrid;
    private LinearLayout entryCard;
    private LinearLayout infoPanel;
    private LinearLayout logPanel;
    private TextView selectedDateTitle;
    private TextView selectedDateSubtitle;
    private TextView editButton;
    private TextView stressValue;
    private TextView stressEvaluation;
    private TextView sleepValue;
    private TextView sleepEvaluation;
    private TextView mealsValue;
    private TextView mealsEvaluation;
    private EditText notesEditText;
    private EditText logNotesEditText;
    private EditText noteSearchInput;
    private TextView saveNotesButton;
    private TextView infoSentimentTag;
    private TextView logSentimentTag;
    private TextView noteSearchButton;
    private TextView noteSearchResult;
    private SeekBar stressSeekBar;
    private SeekBar sleepSeekBar;
    private TextView stressSeekValue;
    private TextView sleepSeekValue;
    private TextView mealCount;
    private ImageButton saveEntryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);
        MenuInferior.configurar(this, 1);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.calendarioRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        bindViews();
        database = new CalendarioDatabase(this);
        loadEntries();
        visibleMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        configureSpinners();
        configureEntryControls();
        buildWeekdays();
        refreshCalendar();
        showSelectedDay();
        DayReminderNotifications.requestPermissionAndSchedule(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == DayReminderNotifications.REQUEST_NOTIFICATIONS
                && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            DayReminderNotifications.scheduleNextReminder(this);
        }
    }

    private void bindViews() {
        monthSpinner = findViewById(R.id.monthSpinner);
        yearSpinner = findViewById(R.id.yearSpinner);
        weekdaysRow = findViewById(R.id.weekdaysRow);
        calendarGrid = findViewById(R.id.calendarGrid);
        entryCard = findViewById(R.id.entryCard);
        infoPanel = findViewById(R.id.infoPanel);
        logPanel = findViewById(R.id.logPanel);
        selectedDateTitle = findViewById(R.id.selectedDateTitle);
        selectedDateSubtitle = findViewById(R.id.selectedDateSubtitle);
        editButton = findViewById(R.id.editButton);
        stressValue = findViewById(R.id.stressValue);
        stressEvaluation = findViewById(R.id.stressEvaluation);
        sleepValue = findViewById(R.id.sleepValue);
        sleepEvaluation = findViewById(R.id.sleepEvaluation);
        mealsValue = findViewById(R.id.mealsValue);
        mealsEvaluation = findViewById(R.id.mealsEvaluation);
        notesEditText = findViewById(R.id.notesEditText);
        logNotesEditText = findViewById(R.id.logNotesEditText);
        noteSearchInput = findViewById(R.id.noteSearchInput);
        saveNotesButton = findViewById(R.id.saveNotesButton);
        infoSentimentTag = findViewById(R.id.infoSentimentTag);
        logSentimentTag = findViewById(R.id.logSentimentTag);
        noteSearchButton = findViewById(R.id.noteSearchButton);
        noteSearchResult = findViewById(R.id.noteSearchResult);
        stressSeekBar = findViewById(R.id.stressSeekBar);
        sleepSeekBar = findViewById(R.id.sleepSeekBar);
        stressSeekValue = findViewById(R.id.stressSeekValue);
        sleepSeekValue = findViewById(R.id.sleepSeekValue);
        mealCount = findViewById(R.id.mealCount);
        saveEntryButton = findViewById(R.id.saveEntryButton);
    }

    private void configureSpinners() {
        List<String> monthNames = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            monthNames.add(capitalize(YearMonth.of(2000, month).getMonth().getDisplayName(TextStyle.FULL, SPANISH)));
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, monthNames);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        int currentYear = LocalDate.now().getYear();
        List<Integer> years = new ArrayList<>();
        for (int year = currentYear - 10; year <= currentYear + 2; year++) {
            years.add(year);
        }
        ArrayAdapter<Integer> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);

        changingSpinners = true;
        monthSpinner.setSelection(visibleMonth.getMonthValue() - 1);
        yearSpinner.setSelection(years.indexOf(visibleMonth.getYear()));
        changingSpinners = false;

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (changingSpinners) return;
                int month = monthSpinner.getSelectedItemPosition() + 1;
                int year = (Integer) yearSpinner.getSelectedItem();
                visibleMonth = YearMonth.of(year, month);
                selectedDate = null;
                refreshCalendar();
                showSelectedDay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };
        monthSpinner.setOnItemSelectedListener(listener);
        yearSpinner.setOnItemSelectedListener(listener);
    }

    private void configureEntryControls() {
        stressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int stress = progress + 1;
                stressSeekValue.setText(stress + " / 10");
                int color = stressColor(stress);
                stressSeekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
                stressSeekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(color));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        sleepSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double hours = progress / 10.0;
                sleepSeekValue.setText(String.format(Locale.getDefault(), "%.1f horas", hours));
                int color = sleepColor(hours);
                sleepSeekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
                sleepSeekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(color));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        findViewById(R.id.minusMeals).setOnClickListener(view -> updateMealCount(-1));
        findViewById(R.id.plusMeals).setOnClickListener(view -> updateMealCount(1));
        addSentimentWatcher(notesEditText, infoSentimentTag);
        addSentimentWatcher(logNotesEditText, logSentimentTag);
        editButton.setOnClickListener(view -> showEditor(entries.get(selectedDate)));
        saveEntryButton.setOnClickListener(view -> saveEntry());
        saveNotesButton.setOnClickListener(view -> saveNote());
        noteSearchButton.setOnClickListener(view -> searchNotesForWord());
        noteSearchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                searchNotesForWord();
                return true;
            }
            return false;
        });
    }

    private void addSentimentWatcher(EditText field, TextView tag) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                updateSentimentTag(text.toString(), tag);
            }
            @Override public void afterTextChanged(Editable editable) { }
        });
        updateSentimentTag(field.getText().toString(), tag);
    }

    private void updateSentimentTag(String note, TextView tag) {
        String label = NoteStressAnalyzer.sentimentLabel(note);
        tag.setText(label);
        int backgroundColor;
        int textColor;
        if (NoteStressAnalyzer.isPositiveLabel(label)) {
            backgroundColor = Color.rgb(224, 244, 231);
            textColor = Color.rgb(42, 133, 82);
        } else if (label.equals("Neutral")) {
            backgroundColor = Color.rgb(237, 243, 245);
            textColor = Color.rgb(96, 119, 127);
        } else {
            backgroundColor = Color.rgb(253, 231, 226);
            textColor = Color.rgb(188, 74, 65);
        }
        GradientDrawable chip = new GradientDrawable();
        chip.setShape(GradientDrawable.RECTANGLE);
        chip.setCornerRadius(dp(14));
        chip.setColor(backgroundColor);
        tag.setBackground(chip);
        tag.setTextColor(textColor);
    }

    private void searchNotesForWord() {
        String normalizedQuery = normalizeForSearch(noteSearchInput.getText().toString().trim());
        String[] queryWords = normalizedQuery.split("[^a-z0-9]+");
        if (queryWords.length != 1 || queryWords[0].isEmpty()) {
            noteSearchResult.setText("Escribe una sola palabra para buscar.");
            noteSearchResult.setVisibility(View.VISIBLE);
            return;
        }
        String query = queryWords[0];

        int matchingEntries = 0;
        int stressTotal = 0;
        for (DayEntry entry : entries.values()) {
            if (containsWholeWord(entry.notes, query)) {
                matchingEntries++;
                stressTotal += entry.stress;
            }
        }

        noteSearchResult.setVisibility(View.VISIBLE);
        if (matchingEntries == 0) {
            noteSearchResult.setText("No hay notas guardadas que contengan «"
                    + noteSearchInput.getText().toString().trim() + "».");
            return;
        }
        double meanStress = stressTotal / (double) matchingEntries;
        noteSearchResult.setText(String.format(Locale.getDefault(),
                "«%s» aparece en %d %s. Estrés medio autoinformado: %.1f / 10 (%s).",
                noteSearchInput.getText().toString().trim(), matchingEntries,
                matchingEntries == 1 ? "nota" : "notas", meanStress,
                stressLabel((int) Math.round(meanStress))));
    }

    private boolean containsWholeWord(String note, String word) {
        for (String token : normalizeForSearch(note).split("[^a-z0-9]+")) {
            if (token.equals(word)) return true;
        }
        return false;
    }

    private String normalizeForSearch(String text) {
        return java.text.Normalizer.normalize(text.toLowerCase(Locale.ROOT),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private void updateMealCount(int difference) {
        selectedMeals = Math.max(0, Math.min(10, selectedMeals + difference));
        mealCount.setText(String.valueOf(selectedMeals));
    }

    private void buildWeekdays() {
        weekdaysRow.removeAllViews();
        String[] names = {"L", "M", "I", "J", "V", "S", "D"};
        for (String name : names) {
            TextView day = new TextView(this);
            day.setLayoutParams(new LinearLayout.LayoutParams(0, dp(36), 1));
            day.setGravity(Gravity.CENTER);
            day.setText(name);
            day.setTextColor(MUTED);
            day.setTextSize(12);
            day.setTypeface(null, Typeface.BOLD);
            weekdaysRow.addView(day);
        }
    }

    private void refreshCalendar() {
        calendarGrid.removeAllViews();
        updateNextDayToLog();

        LocalDate firstOfMonth = visibleMonth.atDay(1);
        int offset = firstOfMonth.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        int daysInMonth = visibleMonth.lengthOfMonth();
        int cellCount = ((offset + daysInMonth + 6) / 7) * 7;
        LocalDate today = LocalDate.now();

        for (int start = 0; start < cellCount; start += 7) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
            for (int column = 0; column < 7; column++) {
                int cell = start + column;
                int dayNumber = cell - offset + 1;
                FrameLayout dayCell = new FrameLayout(this);
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.MATCH_PARENT, 1);
                cellParams.setMargins(dp(2), dp(2), dp(2), dp(2));
                dayCell.setLayoutParams(cellParams);

                if (dayNumber >= 1 && dayNumber <= daysInMonth) {
                    LocalDate date = visibleMonth.atDay(dayNumber);
                    DayEntry entry = entries.get(date);
                    boolean isSelected = date.equals(selectedDate);
                    boolean isFuture = date.isAfter(today);
                    dayCell.setBackground(cellBackground(date, entry, isSelected, isFuture));
                    dayCell.setClickable(!isFuture);
                    dayCell.setFocusable(!isFuture);
                    if (!isFuture) dayCell.setOnClickListener(view -> selectDate(date));

                    TextView number = new TextView(this);
                    FrameLayout.LayoutParams numberParams = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    number.setLayoutParams(numberParams);
                    number.setGravity(Gravity.CENTER);
                    number.setText(String.valueOf(dayNumber));
                    number.setTextSize(14);
                    number.setTypeface(null, isSelected || date.equals(today) ? Typeface.BOLD : Typeface.NORMAL);
                    number.setTextColor(isSelected ? Color.WHITE : isFuture ? Color.rgb(183, 196, 200) : DARK);
                    dayCell.addView(number);

                    if (date.equals(nextDayToLog) && !isFuture) {
                        TextView plus = new TextView(this);
                        FrameLayout.LayoutParams plusParams = new FrameLayout.LayoutParams(dp(19), dp(19),
                                Gravity.END | Gravity.TOP);
                        plusParams.setMargins(0, dp(1), dp(1), 0);
                        plus.setLayoutParams(plusParams);
                        plus.setGravity(Gravity.CENTER);
                        plus.setText("+");
                        plus.setTextSize(14);
                        plus.setTypeface(null, Typeface.BOLD);
                        plus.setTextColor(GREEN);
                        dayCell.addView(plus);
                    }
                }
                row.addView(dayCell);
            }
            calendarGrid.addView(row);
        }
    }

    private GradientDrawable cellBackground(LocalDate date, DayEntry entry, boolean selected, boolean future) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(13));
        if (selected) {
            background.setColor(CYAN);
        } else if (entry != null) {
            background.setColor(heatColor(entry));
        } else if (future) {
            background.setColor(Color.rgb(246, 249, 250));
        } else {
            background.setColor(Color.WHITE);
        }
        if (!selected && date.equals(LocalDate.now())) {
            background.setStroke(dp(1), CYAN);
        }
        return background;
    }

    private void selectDate(LocalDate date) {
        selectedDate = date;
        refreshCalendar();
        showSelectedDay();
    }

    private void showSelectedDay() {
        if (selectedDate == null || selectedDate.isAfter(LocalDate.now())) {
            entryCard.setVisibility(View.GONE);
            return;
        }
        DayEntry entry = entries.get(selectedDate);
        entryCard.setVisibility(View.VISIBLE);
        selectedDateTitle.setText(capitalize(selectedDate.format(
                DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH))));
        selectedDateSubtitle.setText(entry == null
                ? "Día pendiente de registrar"
                : "Resumen personal · estimación orientativa, no clínica");
        if (entry == null) {
            showEditor(null);
        } else {
            editButton.setVisibility(View.VISIBLE);
            infoPanel.setVisibility(View.VISIBLE);
            logPanel.setVisibility(View.GONE);
            populateInfo(entry);
        }
    }

    private void showEditor(DayEntry existing) {
        if (selectedDate == null || selectedDate.isAfter(LocalDate.now())) return;
        editButton.setVisibility(View.GONE);
        infoPanel.setVisibility(View.GONE);
        logPanel.setVisibility(View.VISIBLE);
        DayEntry value = existing == null ? new DayEntry() : existing;
        stressSeekBar.setProgress(clamp(value.stress, 1, 10) - 1);
        sleepSeekBar.setProgress((int) Math.round(Math.max(0, Math.min(12, value.sleepHours)) * 10));
        selectedMeals = Math.max(0, Math.min(10, value.meals));
        mealCount.setText(String.valueOf(selectedMeals));
        logNotesEditText.setText(value.notes);
        updateSeekLabels();
    }

    private void updateSeekLabels() {
        int stress = stressSeekBar.getProgress() + 1;
        double sleep = sleepSeekBar.getProgress() / 10.0;
        stressSeekValue.setText(stress + " / 10");
        sleepSeekValue.setText(String.format(Locale.getDefault(), "%.1f horas", sleep));
        int stressTint = stressColor(stress);
        stressSeekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(stressTint));
        stressSeekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(stressTint));
        int sleepTint = sleepColor(sleep);
        sleepSeekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(sleepTint));
        sleepSeekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(sleepTint));
    }

    private void populateInfo(DayEntry entry) {
        stressValue.setText(entry.stress + " / 10");
        stressEvaluation.setText(stressLabel(entry.stress));
        stressEvaluation.setTextColor(stressColor(entry.stress));
        sleepValue.setText(String.format(Locale.getDefault(), "%.1f horas", entry.sleepHours));
        sleepEvaluation.setText(sleepLabel(entry.sleepHours));
        sleepEvaluation.setTextColor(sleepColor(entry.sleepHours));
        mealsValue.setText(entry.meals + (entry.meals == 1 ? " comida" : " comidas"));
        mealsEvaluation.setText(mealsLabel(entry.meals));
        mealsEvaluation.setTextColor(mealsColor(entry.meals));
        notesEditText.setText(entry.notes);
    }

    private void saveEntry() {
        if (selectedDate == null || selectedDate.isAfter(LocalDate.now())) return;
        DayEntry entry = new DayEntry();
        entry.stress = stressSeekBar.getProgress() + 1;
        entry.sleepHours = sleepSeekBar.getProgress() / 10.0;
        entry.meals = selectedMeals;
        entry.notes = logNotesEditText.getText().toString().trim();
        try {
            database.saveEntry(selectedDate, entry.stress, entry.sleepHours, entry.meals, entry.notes);
        } catch (SQLiteException exception) {
            Toast.makeText(this, "No se pudo guardar el día", Toast.LENGTH_LONG).show();
            return;
        }
        entries.put(selectedDate, entry);
        refreshCalendar();
        showSelectedDay();
        Toast.makeText(this, "Día guardado", Toast.LENGTH_SHORT).show();
    }

    private void saveNote() {
        DayEntry entry = entries.get(selectedDate);
        if (entry == null) return;
        String note = notesEditText.getText().toString().trim();
        try {
            database.saveNote(selectedDate, note);
        } catch (SQLiteException exception) {
            Toast.makeText(this, "No se pudo guardar la nota", Toast.LENGTH_LONG).show();
            return;
        }
        entry.notes = note;
        refreshCalendar();
        Toast.makeText(this, "Nota guardada", Toast.LENGTH_SHORT).show();
    }

    private void updateNextDayToLog() {
        LocalDate today = LocalDate.now();
        if (entries.isEmpty()) {
            nextDayToLog = today;
            return;
        }
        LocalDate firstEntry = entries.keySet().stream()
                .filter(date -> !date.isAfter(today))
                .min(LocalDate::compareTo)
                .orElse(today);
        for (LocalDate date = firstEntry; !date.isAfter(today); date = date.plusDays(1)) {
            if (!entries.containsKey(date)) {
                nextDayToLog = date;
                return;
            }
        }
        nextDayToLog = null;
    }

    private int heatColor(DayEntry entry) {
        double stressRisk = (entry.stress - 1) / 9.0;
        double sleepRisk = Math.max(0, Math.min(1, (7.0 - entry.sleepHours) / 7.0));
        double mealRisk = Math.min(1, Math.abs(entry.meals - 3) / 3.0);
        double metricsRisk = 0.65 * stressRisk + 0.25 * sleepRisk + 0.10 * mealRisk;
        double notesAdjustment = -0.18 * NoteStressAnalyzer.sentiment(entry.notes);
        double risk = Math.max(0, Math.min(1, metricsRisk + notesAdjustment));
        if (risk < 0.34) return Color.rgb(216, 242, 225);
        if (risk < 0.67) return Color.rgb(255, 239, 190);
        return Color.rgb(250, 213, 207);
    }

    private int stressColor(int stress) {
        if (stress <= 3) return Color.rgb(58, 174, 112);
        if (stress <= 6) return Color.rgb(224, 166, 42);
        return Color.rgb(213, 83, 75);
    }

    private int sleepColor(double hours) {
        if (hours >= 7 && hours <= 9) return Color.rgb(58, 174, 112);
        if ((hours >= 6 && hours < 7) || (hours > 9 && hours <= 10)) return Color.rgb(224, 166, 42);
        return Color.rgb(213, 83, 75);
    }

    private int mealsColor(int meals) {
        if (meals == 3) return Color.rgb(58, 174, 112);
        if (meals == 2 || meals == 4) return Color.rgb(224, 166, 42);
        return Color.rgb(213, 83, 75);
    }

    private String stressLabel(int stress) {
        if (stress <= 3) return "Bajo";
        if (stress <= 6) return "Moderado";
        return "Alto";
    }

    private String sleepLabel(double hours) {
        if (hours >= 7 && hours <= 9) return "Suficiente";
        if ((hours >= 6 && hours < 7) || (hours > 9 && hours <= 10)) return "Mejorable";
        return "Insuficiente";
    }

    private String mealsLabel(int meals) {
        if (meals == 3) return "Equilibrado";
        if (meals == 2 || meals == 4) return "Mejorable";
        return "Poco habitual";
    }

    private void loadEntries() {
        android.content.SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        if (preferences.contains(ENTRY_KEY)) {
            String legacyJson = preferences.getString(ENTRY_KEY, "{}");
            if (database.migrateLegacyJson(legacyJson)) {
                preferences.edit().remove(ENTRY_KEY).apply();
            }
        }
        for (Map.Entry<LocalDate, CalendarioDatabase.Entry> storedEntry : database.loadEntries().entrySet()) {
            CalendarioDatabase.Entry stored = storedEntry.getValue();
            DayEntry entry = new DayEntry();
            entry.stress = stored.stress;
            entry.sleepHours = stored.sleepHours;
            entry.meals = stored.meals;
            entry.notes = stored.notes;
            entries.put(storedEntry.getKey(), entry);
        }
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }

    private String capitalize(String value) {
        if (TextUtils.isEmpty(value)) return value;
        return value.substring(0, 1).toUpperCase(SPANISH) + value.substring(1);
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class DayEntry {
        int stress = 5;
        double sleepHours = 7.0;
        int meals = 3;
        String notes = "";
    }
}
