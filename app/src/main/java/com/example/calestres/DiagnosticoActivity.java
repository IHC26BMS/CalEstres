package com.example.calestres;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DiagnosticoActivity extends AppCompatActivity {

    private TextView txtMensajeAlerta, txtResumenEstadistica;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostico);

        txtMensajeAlerta = findViewById(R.id.txtMensajeAlerta);
        txtResumenEstadistica = findViewById(R.id.txtResumenEstadistica);

        // Inicializar la base de datos SQLite local
        dbHelper = new DatabaseHelper(this);

        // Obtener la fecha actual para buscar el registro de hoy
        String fechaHoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Cursor cursor = dbHelper.obtenerRegistroPorFecha(fechaHoy);

        float sueno = 6.0f;
        int comidas = 3;
        int estres = 5;
        String nota = "Sin registros para hoy";

        if (cursor != null && cursor.moveToFirst()) {
            sueno = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUENO));
            comidas = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COMIDAS));
            estres = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ESTRES));
            nota = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTA));
            cursor.close();
        }

        // Motor de Reglas Condicionales (Fase 1 / Proyecto Integrador)
        if (sueno < 5.0f && (estres > 7 || comidas <= 2)) {
            txtMensajeAlerta.setText("Alerta Preventiva: Tus registros indican déficit de sueño (" + sueno + "h) y alto estrés (" + estres + "). ¡Prioriza tu descanso hoy!");
        } else {
            txtMensajeAlerta.setText("¡Buen trabajo! Tus hábitos de sueño y alimentación se mantienen en equilibrio con tu nivel de estrés (" + estres + ").");
        }

        txtResumenEstadistica.setText("Registro de hoy (" + fechaHoy + "):\n• Horas de sueño: " + sueno + "h\n• Comidas: " + comidas + "\n• Nivel de estrés: " + estres + "/10\n• Nota: " + nota);
    }
}