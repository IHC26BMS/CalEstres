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
        String createTable = "CREATE TABLE " + TABLE_REGISTROS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FECHA + " TEXT, " +
                COL_SUENO + " REAL, " +
                COL_COMIDAS + " INTEGER, " +
                COL_ESTRES + " INTEGER, " +
                COL_NOTA + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REGISTROS);
        onCreate(db);
    }

    public boolean guardarRegistro(String fecha, float sueno, int comidas, int estres, String nota) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FECHA, fecha);
        values.put(COL_SUENO, sueno);
        values.put(COL_COMIDAS, comidas);
        values.put(COL_ESTRES, estres);
        values.put(COL_NOTA, nota);

        long result = db.insert(TABLE_REGISTROS, null, values);
        return result != -1;
    }

    public Cursor obtenerRegistroPorFecha(String fecha) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_REGISTROS + " WHERE " + COL_FECHA + " = ?", new String[]{fecha});
    }

    public Cursor obtenerUltimoRegistro() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_REGISTROS + " ORDER BY " + COL_FECHA + " DESC LIMIT 1", null);
    }

    public Cursor obtenerPromediosPorPeriodo(int limiteDias) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT AVG(" + COL_SUENO + ") AS prom_sueno, " +
                "AVG(" + COL_COMIDAS + ") AS prom_comidas, " +
                "AVG(" + COL_ESTRES + ") AS prom_estres " +
                "FROM (SELECT * FROM " + TABLE_REGISTROS + " ORDER BY " + COL_FECHA + " DESC LIMIT " + limiteDias + ")";
        return db.rawQuery(query, null);
    }
}