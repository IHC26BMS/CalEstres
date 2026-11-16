package com.example.calestres;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView txtMensajeAlerta, txtPromedios, txtUltimoRegistro;
    private RadioGroup rgPeriodo;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtMensajeAlerta = findViewById(R.id.txtMensajeAlerta);
        txtPromedios = findViewById(R.id.txtPromedios);
        txtUltimoRegistro = findViewById(R.id.txtUltimoRegistro);
        rgPeriodo = findViewById(R.id.rgPeriodo);

        dbHelper = new DatabaseHelper(this);

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
        Cursor cursor = dbHelper.obtenerUltimoRegistro();

        float sueno = 6.0f;
        int comidas = 3;
        int estres = 5;
        String fecha = "Sin fecha";
        String nota = "Aún no hay registros guardados";

        if (cursor != null && cursor.moveToFirst()) {
            fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FECHA));
            sueno = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUENO));
            comidas = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COMIDAS));
            estres = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTRES));
            nota = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTA));
            cursor.close();
        }

        if (sueno < 5.0f && (estres > 7 || comidas <= 2)) {
            txtMensajeAlerta.setText("⚠️ Alerta Preventiva: En tu último registro presentaste déficit de sueño (" + sueno + "h) y alto estrés (" + estres + "/10). ¡Prioriza descansar hoy!");
        } else {
            txtMensajeAlerta.setText("✨ ¡Buen trabajo! Tus hábitos recientes de sueño (" + sueno + "h) y alimentación se mantienen en equilibrio con tu nivel de estrés (" + estres + "/10).");
        }

        txtUltimoRegistro.setText("Fecha: " + fecha +
                "\n• Horas de sueño: " + sueno + " h" +
                "\n• Comidas en el día: " + comidas +
                "\n• Nivel de estrés: " + estres + "/10" +
                "\n• Nota: " + nota);
    }

    private void cargarPromedios(int dias) {
        Cursor cursor = dbHelper.obtenerPromediosPorPeriodo(dias);

        float promSueno = 6.0f;
        float promComidas = 3.0f;
        float promEstres = 5.0f;

        if (cursor != null && cursor.moveToFirst()) {
            if (!cursor.isNull(0)) {
                promSueno = cursor.getFloat(0);
                promComidas = cursor.getFloat(1);
                promEstres = cursor.getFloat(2);
            }
            cursor.close();
        }

        String etiqueta = (dias == 7) ? "Últimos 7 días" : (dias == 30) ? "Últimos 30 días" : "Último año";
        txtPromedios.setText(String.format(Locale.getDefault(),
                "Periodo: %s\n• Promedio de sueño: %.1f horas\n• Promedio de comidas: %.1f al día\n• Promedio de estrés: %.1f / 10",
                etiqueta, promSueno, promComidas, promEstres));
    }
}