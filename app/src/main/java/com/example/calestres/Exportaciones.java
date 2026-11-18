package com.example.calestres;

import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Exportaciones extends AppCompatActivity {
    private static final int[] PERIODOS_MESES = {1, 3, 6, 12};
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private CalendarioDatabase database;
    private Spinner selectorPeriodo;
    private TextView resumen;
    private ReportData informeActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeMode.applySavedMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exportaciones);
        ThemeMode.applySystemInsets(this, R.id.exportRoot);
        MenuInferior.configurar(this, 2);
        ThemeMode.attach(this, R.id.btnThemeToggle);

        database = new CalendarioDatabase(this);
        selectorPeriodo = findViewById(R.id.spinnerPeriodoExportacion);
        resumen = findViewById(R.id.txtResumenExportacion);
        Button crearPdf = findViewById(R.id.btnCrearPdf);

        String[] periodos = getResources().getStringArray(R.array.export_periods);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, R.layout.spinner_item, periodos);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        selectorPeriodo.setAdapter(adapter);

        selectorPeriodo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cargarResumen(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        crearPdf.setOnClickListener(view -> crearInformePdf());
        cargarResumen(selectorPeriodo.getSelectedItemPosition());
    }

    private void cargarResumen(int posicionPeriodo) {
        int meses = PERIODOS_MESES[posicionPeriodo];
        LocalDate hasta = AppDateTime.today(this);
        LocalDate desde = hasta.minusMonths(meses).plusDays(1);
        List<DailyEntry> registros = new ArrayList<>();

        for (Map.Entry<LocalDate, CalendarioDatabase.Entry> stored : database.loadEntries().entrySet()) {
            LocalDate fecha = stored.getKey();
            if (!fecha.isBefore(desde) && !fecha.isAfter(hasta)) {
                CalendarioDatabase.Entry entry = stored.getValue();
                registros.add(new DailyEntry(fecha, entry.stress, entry.sleepHours, entry.meals, entry.notes));
            }
        }

        informeActual = new ReportData(meses, desde, hasta, registros);
        resumen.setText(formatearResumen(informeActual));
    }

    private String formatearResumen(ReportData report) {
        String periodo = "Periodo: " + FORMATO_FECHA.format(report.desde)
                + " - " + FORMATO_FECHA.format(report.hasta);
        if (report.registros.isEmpty()) {
            return periodo + "\n\nNo hay registros guardados en este periodo.";
        }

        return String.format(Locale.getDefault(),
                "%s\nRegistros: %d días\n\n"
                        + "• Estrés promedio: %.1f / 10\n"
                        + "• Sueño promedio: %.1f horas\n"
                        + "• Comidas promedio: %.1f al día\n"
                        + "• Días con estrés alto (8-10): %d\n"
                        + "• Días con menos de 6 horas de sueño: %d\n"
                        + "• Días con menos de 3 comidas: %d\n"
                        + "• Estrés máximo: %d / 10 (%s)\n"
                        + "• Estrés mínimo: %d / 10 (%s)",
                periodo, report.registros.size(), report.promedioEstres(),
                report.promedioSueno(), report.promedioComidas(), report.estresAlto,
                report.suenoInsuficiente, report.pocasComidas, report.estresMaximo,
                FORMATO_FECHA.format(report.fechaEstresMaximo), report.estresMinimo,
                FORMATO_FECHA.format(report.fechaEstresMinimo));
    }

    private void crearInformePdf() {
        if (informeActual == null) {
            cargarResumen(selectorPeriodo.getSelectedItemPosition());
        }

        PdfDocument documento = new PdfDocument();
        Uri uriGuardada = null;
        try {
            escribirInforme(documento, informeActual);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uriGuardada = guardarEnDescargas(documento);
                Toast.makeText(this, "PDF guardado en Descargas/CalEstres", Toast.LENGTH_LONG).show();
            } else {
                File archivo = guardarEnDocumentosDeLaApp(documento);
                Toast.makeText(this, "PDF guardado en: " + archivo.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (IOException | RuntimeException exception) {
            if (uriGuardada != null) {
                getContentResolver().delete(uriGuardada, null, null);
            }
            Toast.makeText(this, "No se pudo crear el PDF: " + exception.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            documento.close();
        }
    }

    private Uri guardarEnDescargas(PdfDocument documento) throws IOException {
        String nombre = nombreArchivo();
        ContentValues valores = new ContentValues();
        valores.put(MediaStore.MediaColumns.DISPLAY_NAME, nombre);
        valores.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        valores.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/CalEstres");
        valores.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores);
        if (uri == null) {
            throw new IOException("No se pudo crear el archivo en Descargas.");
        }
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) {
                throw new IOException("No se pudo abrir el archivo PDF.");
            }
            documento.writeTo(output);
        } catch (IOException | RuntimeException exception) {
            getContentResolver().delete(uri, null, null);
            throw exception;
        }

        ContentValues publicado = new ContentValues();
        publicado.put(MediaStore.MediaColumns.IS_PENDING, 0);
        try {
            if (getContentResolver().update(uri, publicado, null, null) < 1) {
                throw new IOException("No se pudo publicar el archivo PDF.");
            }
        } catch (IOException | RuntimeException exception) {
            getContentResolver().delete(uri, null, null);
            throw exception;
        }
        return uri;
    }

    private File guardarEnDocumentosDeLaApp(PdfDocument documento) throws IOException {
        File carpeta = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (carpeta == null || (!carpeta.exists() && !carpeta.mkdirs())) {
            throw new IOException("No se pudo acceder a la carpeta de documentos.");
        }
        File archivo = new File(carpeta, nombreArchivo());
        try {
            try (FileOutputStream output = new FileOutputStream(archivo)) {
                documento.writeTo(output);
            }
        } catch (IOException | RuntimeException exception) {
            if (archivo.exists() && !archivo.delete()) {
                throw new IOException("No se pudo eliminar el PDF incompleto.", exception);
            }
            throw exception;
        }
        return archivo;
    }

    private String nombreArchivo() {
        String fechaHora = AppDateTime.now(this)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.getDefault()));
        return "CalEstres_" + fechaHora + ".pdf";
    }

    private void escribirInforme(PdfDocument documento, ReportData report) {
        PdfWriter writer = new PdfWriter(documento, report);
        writer.heading("Resumen de métricas");
        writer.wrapped(formatearResumen(report), 12, Color.rgb(45, 59, 64), false);
        writer.space(10);
        writer.heading("Detalle diario");

        if (report.registros.isEmpty()) {
            writer.wrapped("No hay registros guardados para las fechas seleccionadas.", 12,
                    Color.rgb(45, 59, 64), false);
        } else {
            for (DailyEntry entry : report.registros) {
                String linea = String.format(Locale.getDefault(),
                        "%s   Estrés: %d/10   Sueño: %.1f h   Comidas: %d",
                        FORMATO_FECHA.format(entry.fecha), entry.estres, entry.sueno, entry.comidas);
                writer.wrapped(linea, 11, Color.rgb(23, 59, 70), true);
                if (!entry.nota.trim().isEmpty()) {
                    writer.wrapped("Nota: " + entry.nota.trim(), 10,
                            Color.rgb(90, 105, 110), false);
                }
                writer.rule();
            }
        }
        writer.finish();
    }

    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        super.onDestroy();
    }

    private static final class DailyEntry {
        final LocalDate fecha;
        final int estres;
        final double sueno;
        final int comidas;
        final String nota;

        DailyEntry(LocalDate fecha, int estres, double sueno, int comidas, String nota) {
            this.fecha = fecha;
            this.estres = estres;
            this.sueno = sueno;
            this.comidas = comidas;
            this.nota = nota == null ? "" : nota;
        }
    }

    private static final class ReportData {
        final int meses;
        final LocalDate desde;
        final LocalDate hasta;
        final List<DailyEntry> registros;
        int estresAlto;
        int suenoInsuficiente;
        int pocasComidas;
        int estresMaximo = Integer.MIN_VALUE;
        int estresMinimo = Integer.MAX_VALUE;
        LocalDate fechaEstresMaximo;
        LocalDate fechaEstresMinimo;
        double sumaEstres;
        double sumaSueno;
        int sumaComidas;

        ReportData(int meses, LocalDate desde, LocalDate hasta, List<DailyEntry> registros) {
            this.meses = meses;
            this.desde = desde;
            this.hasta = hasta;
            this.registros = registros;
            for (DailyEntry entry : registros) {
                sumaEstres += entry.estres;
                sumaSueno += entry.sueno;
                sumaComidas += entry.comidas;
                if (entry.estres >= 8) estresAlto++;
                if (entry.sueno < 6) suenoInsuficiente++;
                if (entry.comidas < 3) pocasComidas++;
                if (entry.estres > estresMaximo) {
                    estresMaximo = entry.estres;
                    fechaEstresMaximo = entry.fecha;
                }
                if (entry.estres < estresMinimo) {
                    estresMinimo = entry.estres;
                    fechaEstresMinimo = entry.fecha;
                }
            }
        }

        double promedioEstres() {
            return sumaEstres / registros.size();
        }

        double promedioSueno() {
            return sumaSueno / registros.size();
        }

        double promedioComidas() {
            return (double) sumaComidas / registros.size();
        }
    }

    private static final class PdfWriter {
        private static final int ANCHO_PAGINA = 595;
        private static final int ALTO_PAGINA = 842;
        private static final float MARGEN = 40;
        private final PdfDocument documento;
        private final ReportData report;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private PdfDocument.Page pagina;
        private Canvas canvas;
        private float y;
        private int numeroPagina;

        PdfWriter(PdfDocument documento, ReportData report) {
            this.documento = documento;
            this.report = report;
            nuevaPagina();
        }

        void heading(String texto) {
            texto(texto, 15, Color.rgb(23, 59, 70), true);
        }

        void wrapped(String texto, float tamano, int color, boolean negrita) {
            paint.setTextSize(tamano);
            float ancho = ANCHO_PAGINA - MARGEN * 2;
            StringBuilder linea = new StringBuilder();
            for (String palabra : texto.split("\\s+")) {
                String siguiente = linea.length() == 0 ? palabra : linea + " " + palabra;
                if (paint.measureText(siguiente) > ancho && linea.length() > 0) {
                    texto(linea.toString(), tamano, color, negrita);
                    linea.setLength(0);
                }
                if (linea.length() > 0) linea.append(' ');
                linea.append(palabra);
            }
            if (linea.length() > 0) texto(linea.toString(), tamano, color, negrita);
        }

        void space(float height) {
            y += height;
            comprobarEspacio(18);
        }

        void rule() {
            comprobarEspacio(10);
            paint.setColor(Color.rgb(220, 228, 230));
            paint.setStrokeWidth(1);
            canvas.drawLine(MARGEN, y, ANCHO_PAGINA - MARGEN, y, paint);
            y += 9;
        }

        void finish() {
            documento.finishPage(pagina);
        }

        private void texto(String texto, float tamano, int color, boolean negrita) {
            comprobarEspacio(tamano * 1.7f);
            paint.setColor(color);
            paint.setTextSize(tamano);
            paint.setTypeface(negrita ? Typeface.create("sans-serif", Typeface.BOLD)
                    : Typeface.create("sans-serif", Typeface.NORMAL));
            canvas.drawText(texto, MARGEN, y, paint);
            y += tamano * 1.55f;
        }

        private void comprobarEspacio(float alturaNecesaria) {
            if (y + alturaNecesaria > ALTO_PAGINA - MARGEN) {
                nuevaPagina();
            }
        }

        private void nuevaPagina() {
            if (pagina != null) {
                documento.finishPage(pagina);
            }
            numeroPagina++;
            pagina = documento.startPage(new PdfDocument.PageInfo.Builder(
                    ANCHO_PAGINA, ALTO_PAGINA, numeroPagina).create());
            canvas = pagina.getCanvas();
            y = MARGEN;
            paint.setColor(Color.rgb(32, 167, 184));
            paint.setTextSize(17);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            canvas.drawText("CAL·ESTRES", MARGEN, y, paint);
            paint.setColor(Color.rgb(113, 133, 140));
            paint.setTextSize(9);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            canvas.drawText("Página " + numeroPagina, ANCHO_PAGINA - MARGEN - 55, y, paint);
            y += 21;
            paint.setColor(Color.rgb(23, 59, 70));
            paint.setTextSize(11);
            canvas.drawText("Informe de " + report.meses + " "
                    + (report.meses == 1 ? "mes" : "meses") + " | "
                    + FORMATO_FECHA.format(report.desde) + " - "
                    + FORMATO_FECHA.format(report.hasta), MARGEN, y, paint);
            y += 17;
            paint.setColor(Color.rgb(220, 228, 230));
            canvas.drawLine(MARGEN, y, ANCHO_PAGINA - MARGEN, y, paint);
            y += 20;
        }
    }
}
