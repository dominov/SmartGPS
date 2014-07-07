package com.smartinfo.smartgps;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by CristianPC on 4/07/14.
 */
public class Mi_SQLite_Open_Helper extends SQLiteOpenHelper {

    private static String TAG = "Mi_SQLite_Open_Helper";

    public Mi_SQLite_Open_Helper(Context contexto, String nombre,
                                 SQLiteDatabase.CursorFactory factory, int version) {
        super(contexto, nombre, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Se ejecuta la sentencia SQL de creación de la tabla
        db.execSQL(crear_BD());
    }

    private String crear_BD() {


        StringBuilder sql_creador = new StringBuilder();

        sql_creador.append("CREATE TABLE Coordenadas (");
        sql_creador.append(" latitud TEXT, ");
        sql_creador.append("longitud TEXT, ");
        sql_creador.append("fecha TEXT, ");
        sql_creador.append("hora TEXT, ");
        sql_creador.append("enviado INTEGER ");
        sql_creador.append(")");
        Log.i(TAG, "crear BD: " + sql_creador.toString());
        return sql_creador.toString();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int versionAnterior, int versionNueva) {
        //NOTA: Por simplicidad del ejemplo aquí utilizamos directamente la opción de
        //      eliminar la tabla anterior y crearla de nuevo vacía con el nuevo formato.
        //      Sin embargo lo normal será que haya que migrar datos de la tabla antigua
        //      a la nueva, por lo que este método debería ser más elaborado.

        //Se elimina la versión anterior de la tabla
        db.execSQL("DROP TABLE IF EXISTS Usuarios");

        //Se crea la nueva versión de la tabla
        db.execSQL(crear_BD());
    }
}
