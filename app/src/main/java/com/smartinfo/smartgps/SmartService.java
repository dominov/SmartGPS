package com.smartinfo.smartgps;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class SmartService extends Service {

    private static final String TAG = "SAMRTsERVICE";
    private LocationManager mLocationManagerGPS;
    private LocationManager mLocationManagerRED;
    private MyLocationListener mLocationListener;
    private String _host_name = "";
    private String logstatus2 = "";
    private String _tiempo_envio = "0";
    private String _distancia_envio = "0";
    private String _imei = "";
    private OutputStreamWriter fout = null;
    private Httppostaux post;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //**************** OBTENGO LOS DATOS GUARDADOS EN EL XML ******************
        obtener_preferencias();
        ////******************************************************************************

        Toast.makeText(this, "Servicio creado", Toast.LENGTH_LONG).show();
        Log.i(TAG, "Servicio creado");
        configGPS();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Toast.makeText(this, "Servicio destruido", Toast.LENGTH_LONG).show();
        Log.d(TAG, "Servicio destruido");
    }

    public void setNotification(String mesaje) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notManager = (NotificationManager) getSystemService(ns);
        int icono = android.R.drawable.stat_sys_warning;
        CharSequence textoEstado = "SmartGps!";
        long hora = System.currentTimeMillis();

        Notification notif = new Notification(icono, textoEstado, hora);
        Context contexto = getApplicationContext();
        CharSequence titulo = "SmartGps";
        CharSequence descripcion = mesaje;

        Intent notIntent = new Intent(contexto,Activity_Principal.class);

        PendingIntent contIntent = PendingIntent.getActivity(contexto, 0, notIntent, 0);

        notif.setLatestEventInfo(contexto, titulo, descripcion, contIntent);
        notif.flags |= Notification.FLAG_AUTO_CANCEL;
        notManager.notify(12, notif);
    }

    private boolean comprobar_conexion() {

        boolean conexion;
        ConnectivityManager cm = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        conexion = (netInfo != null && netInfo.isConnectedOrConnecting());


        return conexion;
    }


    private void configGPS() {
        Log.i(TAG, "Entro al ConfiGPS");

        int tiempo = Integer.parseInt(_tiempo_envio);
        Log.i(TAG, "time " + tiempo);

        int distancia = Integer.parseInt(_distancia_envio);
        Log.i(TAG, "_distancia_envio " + distancia);

        mLocationManagerGPS = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManagerRED = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (mLocationManagerGPS.isProviderEnabled(LocationManager.GPS_PROVIDER)) {//hay gps
            //provider=true;

            mLocationListener = new MyLocationListener("GPS");
            mLocationManagerGPS.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, tiempo, distancia, new MyLocationListener("GPSs"));


          /*  if (mLocationManagerRED.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

                mLocationListener = new MyLocationListener("RED");

                mLocationManagerRED.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, tiempo,distancia, new MyLocationListener("RED"));
            }*/

        } else if (mLocationManagerRED.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            //provider=true;


            mLocationListener = new MyLocationListener("RED");

            mLocationManagerRED.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, tiempo, distancia, new MyLocationListener("RED"));


        } else {// no hay gps
            //provider=false;
            //setxml("0","0");
            setNotification("No se a podido establecer ubicacion configure uno de los dos protocolos");
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);

        }
    }

/*
************************************************************************************************
* Gestionar Preferencias
 */

    public void obtener_preferencias() {
        Log.i(TAG, "obtener_preferencias");

        SharedPreferences prefs = getSharedPreferences("MYSMARTGPS_preferencias",
                Context.MODE_PRIVATE);

        _host_name = prefs.getString("host_name", "***");
        _imei = prefs.getString("imei", "***");
        _tiempo_envio = prefs.getString("tiempo", "***");
        _distancia_envio = prefs.getString("distancia", "***");

    }

    public boolean guardar_preferencias(String domine, String imei, String tiempo_envio, String distancia_envio) {
        Log.i(TAG, "guardar_preferencias");
        SharedPreferences prefs = getSharedPreferences("MYSMARTGPS_preferencias",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("host_name", domine);
        editor.putString("imei", imei);
        editor.putString("tiempo", tiempo_envio);
        editor.putString("distancia", distancia_envio);


        boolean a = editor.commit();

        return a;
    }

    private class MyLocationListener implements LocationListener {
        private String tipos;

        private MyLocationListener(String tipos) {
            this.tipos = tipos;
        }

        @Override
        public void onLocationChanged(Location loc) {
            if (loc != null) {

                String latitud = String.valueOf(loc.getLatitude());
                String longitud = String.valueOf(loc.getLongitude());
                setNotification("S " + tipos + ": " + latitud + " " + longitud);



                subir_datos(latitud, longitud);

            }

        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
            Log.d(TAG, "Proveedor desconectado");
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
            Log.d(TAG, "Proveedor conectado");
        }

        @Override
        public void onStatusChanged(String provider, int status,
                                    Bundle extras) {
            // TODO Auto-generated method stub
            Log.d(TAG, "Proveedor estado: " + status);
        }
    }

    private void subir_datos(String latitud, String longitud) {
        Log.i(TAG, "subir_datos");

        int logstatus = -1;


        ArrayList<NameValuePair> postparameterssend2 = new ArrayList<NameValuePair>();
        postparameterssend2.add(new BasicNameValuePair("lat", latitud));
        postparameterssend2.add(new BasicNameValuePair("long", longitud));
        postparameterssend2.add(new BasicNameValuePair("id_usuario", _imei));
        JSONArray jata = null;

        String urlserver = "http://" + _host_name + "/discorralitodepiedra/sigep/movil/insertar_coordenadas.php";
        try {
            if (comprobar_conexion()) {

                jata = post.getserverdata(postparameterssend2, urlserver, _tiempo_envio, _distancia_envio);

                sincronizar_BD_Local();

            }

            /******* mandar datos y recibir*********/


            Log.i(TAG, "  servicio repsuesta : " + jata);
        } catch (Exception e) {
            Log.e(TAG, "HTTPPOST server " + e.toString() + " " + postparameterssend2.toString() + " " + urlserver);
        }
        if (jata != null && jata.length() > 0) {
            JSONObject jdata_object;
            //JSONObject jdata_url;
            try {

                jdata_object = jata.getJSONObject(0);
                logstatus = jdata_object.getInt("logstatus");
                Log.i(TAG, "logstatus " + logstatus);

                logstatus2 = jdata_object.getString("logstatus2");

                String tiempo_envio = jdata_object.getString("tiempo");
                String distancia_envio = jdata_object.getString("distancia");

                Log.i(TAG, "envio " + tiempo_envio);

                Log.i(TAG, "envio " + distancia_envio);


                if ((_host_name.equals(logstatus2)) && (tiempo_envio.equals(_tiempo_envio)) && (distancia_envio.equals(_distancia_envio))) {
                    Log.i("Valores Iguales Tmp, Dist: " + tiempo_envio + "--" + distancia_envio, "URL: " + logstatus2);
                } else {
                    guardar_preferencias(logstatus2, _imei, _tiempo_envio, _distancia_envio);

                }

            } catch (JSONException e) {
                e.printStackTrace();

            }

            if (logstatus == 0) {
                Log.e("loginstatus", "invalido");
                guardar_BD_local(latitud, longitud, false);


            } else {
                Log.i(TAG, "loginstatus valido");
                guardar_BD_local(latitud, longitud, true);

            }

        } else {
            Log.e(TAG, "JSON ERROR");
            guardar_BD_local(latitud, longitud, false);

        }
    }

    /*
    ****************************************************************
    * Gestion de BAse de DAtos
     */
    private void sincronizar_BD_Local() {

        Mi_SQLite_Open_Helper mi_sqLite_open_helper =
                new Mi_SQLite_Open_Helper(this, "BDCoordenadas", null, 1);

        SQLiteDatabase db = mi_sqLite_open_helper.getWritableDatabase();

        if (db != null) {
            String[] campos = new String[]{"latitud", "longitud", "fecha", "hora", "enviado"};
            String[] args = new String[]{"0"};

            Cursor c = db.rawQuery("SELECT latitud, longitud, fecha, hora, enviado FROM Coordenadas ", null);
            mostrar_BD(c);


            if (c.getCount() > 0) {

                Dao_Coordenadas dao_coordenadas = new Dao_Coordenadas();

                String urlserver = "http://" + _host_name + "/discorralitodepiedra/sigep/movil/insertar_coordenadas.php";

                boolean res = dao_coordenadas.insertar(c, urlserver);


                if (res) {
                    db.execSQL("Delete From Coordenadas");
                }
            }

            db.close();

        }
    }

    private void mostrar_BD(Cursor c) {
        Log.i(TAG, "mostrar_BD");
        if (c.moveToFirst()) {
            do {

                Log.w(TAG, c.getString(0) + " - " + c.getString(1) + " - " + c.getString(2) + " - " + c.getString(3) + " - " + c.getInt(4));
            } while (c.moveToNext());
        }
    }

    private void guardar_BD_local(String latitud, String longitud, boolean b) {

        Date date = new Date();
        SimpleDateFormat formatoDeFecha = new SimpleDateFormat(
                "yyyy-MM-dd");
        String fecha = formatoDeFecha.format(date);

        formatoDeFecha = new SimpleDateFormat(
                "hh:mm:ss");
        String hora = formatoDeFecha.format(date);
        int enviado = 0;
        if (b) {
            enviado = 1;
        }

        Log.i(TAG, "INSERT INTO Coordenadas (latitud,longitud,fecha,hora,enviado) " +
                "VALUES ('" + latitud + "', '" + longitud + "', '" + fecha + "', '" + hora + "', " + enviado + "')");


//Creamos el registro a insertar como objeto ContentValues
        ContentValues nuevoRegistro = new ContentValues();
        nuevoRegistro.put("latitud", latitud);
        nuevoRegistro.put("longitud", longitud);
        nuevoRegistro.put("fecha", fecha);
        nuevoRegistro.put("hora", hora);
        nuevoRegistro.put("enviado", enviado);


        //Abrimos la base de datos 'BDCoordenadas' en modo escritura
        Mi_SQLite_Open_Helper mi_sqLite_open_helper =
                new Mi_SQLite_Open_Helper(this, "BDCoordenadas", null, 1);

        SQLiteDatabase db = mi_sqLite_open_helper.getWritableDatabase();

        //Si hemos abierto correctamente la base de datos
        if (db != null) {
            //Insertamos el registro en la base de datos
            db.insert("Coordenadas", null, nuevoRegistro);
            db.close();
        }
    }


}
