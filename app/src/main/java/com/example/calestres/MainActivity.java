package com.example.calestres;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TextView txtMensajeAlerta;
    private TextView txtPromedios;
    private TextView txtUltimoRegistro;
    private RadioGroup rgPeriodo;
    private CalendarioDatabase database;
    private Map<LocalDate, CalendarioDatabase.Entry> registros;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeMode.applySavedMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ThemeMode.applySystemInsets(this, R.id.mainRoot);
        MenuInferior.configurar(this, 0);
        ThemeMode.attach(this, R.id.btnThemeToggle);

        txtMensajeAlerta = findViewById(R.id.txtMensajeAlerta);
        txtPromedios = findViewById(R.id.txtPromedios);
        txtUltimoRegistro = findViewById(R.id.txtUltimoRegistro);
        rgPeriodo = findViewById(R.id.rgPeriodo);

        database = new CalendarioDatabase(this);
        registros = database.loadEntries();

        cargarUltimoDiaYDiagnostico();
        cargarPromedios(7);

        rgPeriodo.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSemana) {
                cargarPromedios(7);
            } else if (checkedId == R.id.rbMes) {
                cargarPromedios(30);
            } else if (checkedId == R.id.rbAnio) {
                cargarPromedios(365);
            }
        });
    }

    private void cargarUltimoDiaYDiagnostico() {
        if (registros.isEmpty()) {
            txtMensajeAlerta.setText("Aún no hay registros para generar un diagnóstico.");
            txtUltimoRegistro.setText(
                    "Registra un día en el calendario para ver aquí tus métricas.");
            return;
        }

        Map.Entry<LocalDate, CalendarioDatabase.Entry> ultimoRegistro =
                new TreeMap<>(registros).lastEntry();
        LocalDate fecha = ultimoRegistro.getKey();
        CalendarioDatabase.Entry entry = ultimoRegistro.getValue();

        if (entry.sleepHours < 5.0 && (entry.stress > 7 || entry.meals <= 2)) {
            txtMensajeAlerta.setText(String.format(Locale.getDefault(),
                    "⚠️ Alerta preventiva: el último registro muestra %.1f h de sueño, "
                            + "%d/10 de estrés y %d comidas. Prioriza tu descanso y bienestar.",
                    entry.sleepHours, entry.stress, entry.meals));
        } else {
            txtMensajeAlerta.setText(String.format(Locale.getDefault(),
                    "✨ Último registro: %.1f h de sueño, %d comidas y %d/10 de estrés.",
                    entry.sleepHours, entry.meals, entry.stress));
        }

        txtUltimoRegistro.setText(String.format(Locale.getDefault(),
                "Fecha: %s\n• Horas de sueño: %.1f h\n• Comidas en el día: %d"
                        + "\n• Nivel de estrés: %d/10\n• Nota: %s",
                FORMATO_FECHA.format(fecha), entry.sleepHours, entry.meals, entry.stress,
                entry.notes.isEmpty() ? "Sin nota" : entry.notes));
    }

    private void cargarPromedios(int dias) {
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.minusDays(dias - 1L);
        int cantidad = 0;
        double totalSueno = 0;
        double totalComidas = 0;
        double totalEstres = 0;

        for (Map.Entry<LocalDate, CalendarioDatabase.Entry> stored : registros.entrySet()) {
            if (stored.getKey().isBefore(desde) || stored.getKey().isAfter(hasta)) continue;
            CalendarioDatabase.Entry entry = stored.getValue();
            totalSueno += entry.sleepHours;
            totalComidas += entry.meals;
            totalEstres += entry.stress;
            cantidad++;
        }

        String etiqueta = (dias == 7) ? "Últimos 7 días"
                : (dias == 30) ? "Últimos 30 días" : "Último año";
        if (cantidad == 0) {
            txtPromedios.setText("Periodo: " + etiqueta
                    + "\nAún no hay registros guardados en este periodo.");
            return;
        }

        txtPromedios.setText(String.format(Locale.getDefault(),
                "Periodo: %s (%d registros)\n• Promedio de sueño: %.1f horas"
                        + "\n• Promedio de comidas: %.1f al día"
                        + "\n• Promedio de estrés: %.1f / 10",
                etiqueta, cantidad, totalSueno / cantidad, totalComidas / cantidad,
                totalEstres / cantidad));
    }

    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        super.onDestroy();
    }
}