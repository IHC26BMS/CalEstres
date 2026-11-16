package com.example.calestres;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "cal_estres.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_REGISTROS = "registros";
    public static final String COL_ID = "id";
    public static final String COL_FECHA = "fecha";
    public static final String COL_SUENO = "sueno";
    public static final String COL_COMIDAS = "comidas";
    public static final String COL_ESTRES = "estres";
    public static final String COL_NOTA = "nota";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_REGISTROS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FECHA + " TEXT UNIQUE, " +
                COL_SUENO + " REAL, " +
                COL_COMIDAS + " INTEGER, " +
                COL_ESTRES + " INTEGER, " +
                COL_NOTA + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REGISTROS);
        onCreate(db);
    }

    // Método para guardar o actualizar el registro diario (Baja Fricción)
    public boolean guardarRegistro(String fecha, float sueno, int comidas, int estres, String nota) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FECHA, fecha);
        values.put(COL_SUENO, sueno);
        values.put(COL_COMIDAS, comidas);
        values.put(COL_ESTRES, estres);
        values.put(COL_NOTA, nota);

        long result = db.insertWithOnConflict(TABLE_REGISTROS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    // Método para consultar los datos de un día específico
    public Cursor obtenerRegistroPorFecha(String fecha) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_REGISTROS,
                null,
                COL_FECHA + " = ?",
                new String[]{fecha},
                null,
                null,
                null);
    }
}
