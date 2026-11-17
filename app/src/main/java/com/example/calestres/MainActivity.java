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
        txtMensajeAlerta.setText(evaluarBienestarGeneral());
        if (registros.isEmpty()) {
            txtUltimoRegistro.setText(
                    "Registra un día en el calendario para ver aquí tus métricas.");
            return;
        }

        Map.Entry<LocalDate, CalendarioDatabase.Entry> ultimoRegistro =
                new TreeMap<>(registros).lastEntry();
        LocalDate fecha = ultimoRegistro.getKey();
        CalendarioDatabase.Entry entry = ultimoRegistro.getValue();

        txtUltimoRegistro.setText(String.format(Locale.getDefault(),
                "Fecha: %s\n• Horas de sueño: %.1f h\n• Comidas en el día: %d"
                        + "\n• Nivel de estrés: %d/10\n• Nota: %s",
                FORMATO_FECHA.format(fecha), entry.sleepHours, entry.meals, entry.stress,
                entry.notes.isEmpty() ? "Sin nota" : entry.notes));
    }

    private String evaluarBienestarGeneral() {
        String aviso = "\n\nOrientación basada en tus registros; no es un diagnóstico médico.";
        if (registros.isEmpty()) {
            return "Aún no hay registros para valorar tu progreso." + aviso;
        }

        LocalDate hoy = LocalDate.now();
        PeriodMetrics recientes = resumirPeriodo(hoy.minusDays(6), hoy);
        if (recientes.cantidad < 3) {
            return "Seguimiento inicial\n"
                    + "Hay " + recientes.cantidad + " registros en los últimos 7 días. "
                    + "Añade al menos 3 registros recientes para observar una tendencia."
                    + aviso;
        }

        PeriodMetrics anteriores = resumirPeriodo(hoy.minusDays(13), hoy.minusDays(7));
        String detalle = String.format(Locale.getDefault(),
                "Últimos 7 días (%d registros): estrés promedio %.1f/10, "
                        + "sueño %.1f h y comidas %.1f al día.",
                recientes.cantidad, recientes.promedioEstres(), recientes.promedioSueno(),
                recientes.promedioComidas());

        if (recientes.promedioEstres() >= 8
                || (recientes.promedioEstres() >= 7 && recientes.promedioSueno() < 6)) {
            return "Se recomienda hablar con un profesional\n" + detalle
                    + "\nTus registros reflejan estrés alto o estrés elevado junto con poco "
                    + "sueño. Si persiste, te preocupa "
                    + "o afecta tu vida diaria, considera conversar con un profesional de salud."
                    + aviso;
        }

        if (anteriores.cantidad >= 3) {
            double cambioEstres = recientes.promedioEstres() - anteriores.promedioEstres();
            double cambioSueno = recientes.promedioSueno() - anteriores.promedioSueno();
            boolean empeorando = (cambioEstres >= 1 && recientes.promedioEstres() >= 6)
                    || (cambioSueno <= -1 && recientes.promedioSueno() < 7);
            if (empeorando) {
                return "Empeorando\n" + detalle
                        + "\nEn comparación con la semana anterior, algunas métricas han "
                        + "cambiado desfavorablemente. Observa cómo te sientes y busca apoyo "
                        + "si la tendencia continúa."
                        + aviso;
            }

            boolean mejorando = cambioEstres <= -1 || cambioSueno >= 1;
            if (mejorando) {
                return "Mejorando\n" + detalle
                        + "\nAlgunas métricas han mejorado respecto a la semana anterior. "
                        + "Sigue observando tus hábitos."
                        + aviso;
            }

            if (vaBien(recientes)) {
                return "Todo bien\n" + detalle
                        + "\nTus registros recientes muestran niveles generalmente favorables."
                        + aviso;
            }
            return "Estable\n" + detalle
                    + "\nNo se observa un cambio marcado frente a la semana anterior."
                    + aviso;
        }

        if (vaBien(recientes)) {
            return "Vas bien\n" + detalle
                    + "\nTodavía no hay suficientes registros de la semana anterior para "
                    + "comparar una tendencia."
                    + aviso;
        }
        return "Seguimiento\n" + detalle
                + "\nAún falta información de la semana anterior para valorar si hay una "
                + "tendencia."
                + aviso;
    }

    private boolean vaBien(PeriodMetrics metrics) {
        return metrics.promedioEstres() <= 4.5
                && metrics.promedioSueno() >= 7
                && metrics.promedioComidas() >= 2.5;
    }

    private PeriodMetrics resumirPeriodo(LocalDate desde, LocalDate hasta) {
        PeriodMetrics metrics = new PeriodMetrics();
        for (Map.Entry<LocalDate, CalendarioDatabase.Entry> stored : registros.entrySet()) {
            if (stored.getKey().isBefore(desde) || stored.getKey().isAfter(hasta)) continue;
            CalendarioDatabase.Entry entry = stored.getValue();
            metrics.sumaEstres += entry.stress;
            metrics.sumaSueno += entry.sleepHours;
            metrics.sumaComidas += entry.meals;
            metrics.cantidad++;
        }
        return metrics;
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

    private static final class PeriodMetrics {
        int cantidad;
        double sumaEstres;
        double sumaSueno;
        double sumaComidas;

        double promedioEstres() {
            return sumaEstres / cantidad;
        }

        double promedioSueno() {
            return sumaSueno / cantidad;
        }

        double promedioComidas() {
            return sumaComidas / cantidad;
        }
    }
}