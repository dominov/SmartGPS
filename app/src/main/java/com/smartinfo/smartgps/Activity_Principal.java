package com.smartinfo.smartgps;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Xml;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;

import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import smartgps.smartinfo.com.smartgps.R;


public class Activity_Principal extends Activity implements Runnable {

    private static final int MOSTRAR_POS = 0;
    private static final String TAG = "ACTIVY_PRINCIPAL";
    private static final int MOSTRAR_ESTADO = 1;

    // tiempo que debe tardar en cada actualizacion de la BD
    private long _lapsus_de_carga = 180000;
    private long _tiempo_de_ultima_carga;

    private String _host_name = "";
    private String _imei = "";
    private String _tiempo_envio = "";
    private String _distancia_envio = "";

    private OutputStreamWriter fout = null;
    private XmlSerializer ser = Xml.newSerializer();
    private EditText url = null;
    private TextView label_url = null;
    private Button coonfButton;
    private ToggleButton _swt_GPS;
    private Httppostaux post;
    private Timer timer = null;
    private boolean provider = false;
    private Thread thread = null;
    private TextView outlat, outlong, _lbl_estado;


    private LocationManager mLocationManagerGPS;
    private LocationManager mLocationManagerRED;
    private MyLocationListener myLocationListenerGPS;
    private MyLocationListener myLocationListenerRED;
    private Location currentLocation = null;
    private String current_estado = null;

    @Override

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_principal);
        boolean exit = val_preferencias();

        inicializar();

        post = new Httppostaux();
        timer = new Timer();

        if (!exit) {
            outlat.setVisibility(View.INVISIBLE);
            outlong.setVisibility(View.INVISIBLE);
            _swt_GPS.setVisibility(View.INVISIBLE);
            Log.i(TAG, "pRIMERAA");
            //---Bloque de configuracion si no esta configurado
            coonfButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    getHostName();
                    //   startService();
                }
            });
            //---Fin Bloque configuracion
        } else {
            ocultar_campos_configuracion();
            outlat.setVisibility(View.VISIBLE);
            outlong.setVisibility(View.VISIBLE);
            _swt_GPS.setVisibility(View.VISIBLE);

            obtener_preferencia();
            Log.i(TAG, "tiempo " + _tiempo_envio);
            Log.i(TAG, "distan " + _tiempo_envio);


            activar_hilo();
            // startService();
        }

    }

    private void inicializar() {
        _tiempo_de_ultima_carga = SystemClock.elapsedRealtime() - _lapsus_de_carga;

        coonfButton = (Button) findViewById(R.id.btn_config);
        label_url = (TextView) findViewById(R.id.label_url);
        url = (EditText) findViewById(R.id.text_url);
        outlat = (TextView) findViewById(R.id.outlat);
        outlong = (TextView) findViewById(R.id.outlong);
        _lbl_estado = (TextView) findViewById(R.id.lbl_estado);
        _swt_GPS = (ToggleButton) findViewById(R.id.tBtn_act_des);

        _swt_GPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (b) {
                    mLocationManagerGPS.removeUpdates(myLocationListenerGPS);
                    mLocationManagerRED.removeUpdates(myLocationListenerRED);
                    stop();

                    setNotification("GPS detenido" , "Stop");

                } else {
Log.w(TAG,"arraanca");
                    activar_hilo();
                }
            }
        });
    }

    public void stop() {
        thread.interrupt();
    }

    private boolean comprobar_conexion() {

        boolean conexion;
        ConnectivityManager cm = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        conexion = (netInfo != null && netInfo.isConnectedOrConnecting());


        return conexion;
    }

    private boolean es_tiempo() {

        return (SystemClock.elapsedRealtime() - _tiempo_de_ultima_carga) > _lapsus_de_carga;
    }


    void getHostName() {

        _tiempo_envio = "0";
        _distancia_envio = "0";
        _host_name = url.getText().toString();
        if (_host_name.length() == 0)
            Toast.makeText(this, "Debe ingresar un Nombre de Dominio", Toast.LENGTH_SHORT).show();
        else {
            TelephonyManager TelephonyMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            _imei = TelephonyMgr.getDeviceId();

            boolean res = guardar_preferncias(_host_name, _imei, _tiempo_envio, _distancia_envio);
            if (res) {
                ocultar_campos_configuracion();
                activar_hilo();
                outlat.setVisibility(View.VISIBLE);
                outlong.setVisibility(View.VISIBLE);
                _lbl_estado.setVisibility(View.VISIBLE);
                _swt_GPS.setVisibility(View.VISIBLE);
            }
        }
    }


    public void ocultar_campos_configuracion() {
        Toast.makeText(this, "Ubicando datos", Toast.LENGTH_SHORT).show();
        label_url.setVisibility(View.INVISIBLE);
        url.setVisibility(View.INVISIBLE);
        coonfButton.setVisibility(View.INVISIBLE);
    }

    public void setNotification(String mesaje, String t) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notManager = (NotificationManager) getSystemService(ns);
        int icono = android.R.drawable.stat_sys_warning;
        CharSequence textoEstado = "SmarGPS " + t;
        long hora = System.currentTimeMillis();

        Notification notif = new Notification(icono, textoEstado, hora);
        Context contexto = getApplicationContext();
        CharSequence titulo = "SmarGPS " + t;
        CharSequence descripcion = mesaje;

        Intent notIntent = new Intent(contexto, Activity_Principal.class);

        PendingIntent contIntent = PendingIntent.getActivity(contexto, 0, notIntent, 0);

        notif.setLatestEventInfo(contexto, titulo, descripcion, contIntent);
        notif.flags |= Notification.FLAG_AUTO_CANCEL;
        notManager.notify(12, notif);
    }


    public void activar_hilo() {
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                //writeSignalGPS();
                validar_provider();
            }
        }, 0, 10000);
    }

    private void validar_provider() {
        if (!provider) {
            writeSignalGPS();
        }
    }

    private void writeSignalGPS() {


        handler.sendEmptyMessage(MOSTRAR_POS);

        thread = new Thread(this);
        thread.start();

    }

    @Override
    public void run() {
        Log.i(TAG, "RUnnn Activy_Principal");
        capturar_posicion();

    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case MOSTRAR_POS:
                    if (currentLocation != null) {
                        double latitud = currentLocation.getLatitude();
                        double longitud = currentLocation.getLongitude();
                        String lat = "Latitude: " + latitud;
                        String lon = "Longitude: " + longitud;

                        outlat.setText(lat);
                        outlong.setText(lon);

                    }

                    break;
                case MOSTRAR_ESTADO:
                    if (current_estado != null) {
                        _lbl_estado.setText(current_estado);

                    }

                    break;

            }
        }
    };

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

                setNotification(tipos + ": " + latitud + " " + longitud, "Actualizacion de Pocicion");
                set_pos_actual(loc);
                handler.sendEmptyMessage(MOSTRAR_POS);

                if (es_tiempo()) {
                    Log.w(TAG, "es  tiempo");
                    subir_datos(latitud, longitud);
                } else {
                    // Log.w(TAG,"no es tiempo");

                }

                /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

            }

        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider, int status,
                                    Bundle extras) {

            set_estado_provider(provider, status);
            handler.sendEmptyMessage(MOSTRAR_ESTADO);


        }
    }

    private void capturar_posicion() {


        int tiempo = Integer.parseInt(_tiempo_envio);
        int distancia = Integer.parseInt(_distancia_envio);

        Log.w(TAG, "t: " + tiempo + " d: " + distancia);

        mLocationManagerGPS = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationManagerRED = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (mLocationManagerGPS.isProviderEnabled(LocationManager.GPS_PROVIDER)) {//hay gps
            provider = true;
            Looper.prepare();
            myLocationListenerGPS = new MyLocationListener("GPS");
            mLocationManagerGPS.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, tiempo, distancia, myLocationListenerGPS);


            if (mLocationManagerRED.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                provider = true;
                myLocationListenerRED = new MyLocationListener("RED");
                mLocationManagerRED.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, tiempo, distancia, myLocationListenerRED);

            }
            Looper.loop();
            Looper.myLooper().quit();

        } else if (mLocationManagerRED.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Looper.prepare();
            myLocationListenerRED = new MyLocationListener("RED");
            mLocationManagerRED.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, tiempo, distancia, myLocationListenerRED);
            Looper.loop();
            Looper.myLooper().quit();

        } else {// no hay gps
            provider = false;

            setNotification("No se a podido establecer ubicacion configure uno de los dos protocolos", "error");
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);

        }

        Log.i(TAG, "Pasosssss");
    }

    private void set_pos_actual(Location loc) {
        currentLocation = loc;
    }

    private void set_estado_provider(String provider, int status) {
        current_estado = provider + " : " + status;
    }


    private void startService() {
        Intent svc = new Intent(this, SmartService.class);
        startService(svc);
    }

    private boolean subir_datos(String latitud, String longitud) {

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

        } catch (Exception e) {
            Log.e(TAG, "server " + e.toString() + " " + postparameterssend2.toString() + " " + urlserver);
        }
        if (jata != null && jata.length() > 0) {
            _tiempo_de_ultima_carga = SystemClock.elapsedRealtime();
            JSONObject jdata_object;
            try {

                jdata_object = jata.getJSONObject(0);
                logstatus = jdata_object.getInt("logstatus");
                Log.i(TAG, "logstatus " + logstatus);
            } catch (JSONException e) {
                e.printStackTrace();

            }

            if (logstatus == 0) {
                Log.e("loginstatus", "invalido");
                guardar_BD_local(latitud, longitud, false);
                return false;

            } else {

                Log.i(TAG, "loginstatus valido");
                guardar_BD_local(latitud, longitud, true);
                return true;
            }

        } else {
            Log.e("JSON", "ERROR");
            guardar_BD_local(latitud, longitud, false);
            return false;
        }


    }
    /*
    ********************************************************************************************
    * Preferencias
     */

    public boolean val_preferencias() {

        SharedPreferences prefs = getSharedPreferences("MYSMARTGPS_preferencias",
                Context.MODE_PRIVATE);

        String h = prefs.getString("host_name", "***");
        String i = prefs.getString("imei", "***");
        String t = prefs.getString("tiempo", "***");
        String d = prefs.getString("distancia", "***");

        if (h.equals("***")) {
            return false;
        }
        if (i.equals("***")) {
            return false;
        }
        if (t.equals("***")) {
            return false;
        }
        if (d.equals("***")) {
            return false;
        }
        return true;
    }

    public void obtener_preferencia() {

        SharedPreferences prefs = getSharedPreferences("MYSMARTGPS_preferencias",
                Context.MODE_PRIVATE);

        _host_name = prefs.getString("host_name", "***");
        _imei = prefs.getString("imei", "***");
        _tiempo_envio = prefs.getString("tiempo", "***");
        _distancia_envio = prefs.getString("distancia", "***");
    }

    private boolean guardar_preferncias(String domine, String imei, String tiempo_envio, String distancia_envio) {

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

    /*
    ***********************************************************************************************
    * BASE de DATOS LOCAL
     */

    private void sincronizar_BD_Local() {

        Log.i(TAG, "sincronizar_BD_Local");
        Mi_SQLite_Open_Helper mi_sqLite_open_helper =
                new Mi_SQLite_Open_Helper(this, "BDCoordenadas", null, 1);

        SQLiteDatabase db = mi_sqLite_open_helper.getWritableDatabase();

        if (db != null) {


            Cursor c = db.rawQuery("SELECT latitud, longitud, fecha, hora, enviado FROM Coordenadas ", null);

            mostrar_BD(c);
            db.close();

            if (c.getCount() > 0) {

                Dao_Coordenadas dao_coordenadas = new Dao_Coordenadas();

                String urlserver = "http://" + _host_name + "/discorralitodepiedra/sigep/movil/insertar_coordenadas.php";

                boolean res = dao_coordenadas.insertar(c, urlserver);


                if (res) {
                    db.execSQL("Delete From Coordenadas");
                }
            }

        }
    }

    private void mostrar_BD(Cursor c) {

        Log.i(TAG, "mostrar_BD");

        if (c.moveToFirst()) {
            do {
                Log.w(TAG, "1");
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

        String sql = "INSERT INTO Coordenadas (latitud,longitud,fecha,hora,enviado) " +
                "VALUES ('" + latitud + "', '" + longitud + "', '" + fecha + "', '" + hora + "', " + enviado + ")";
        Log.i(TAG, sql);

        ContentValues nuevoRegistro = new ContentValues();
        nuevoRegistro.put("latitud", latitud);
        nuevoRegistro.put("longitud", longitud);

        nuevoRegistro.put("enviado", enviado);
        nuevoRegistro.put("fecha", fecha);
        nuevoRegistro.put("hora", hora);

        Mi_SQLite_Open_Helper mi_sqLite_open_helper =
                new Mi_SQLite_Open_Helper(this, "BDCoordenadas", null, 1);

        SQLiteDatabase db = mi_sqLite_open_helper.getWritableDatabase();


        if (db != null) {

            db.execSQL(sql);
            db.close();
        }
    }


}
