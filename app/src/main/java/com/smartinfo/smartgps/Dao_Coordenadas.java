package com.smartinfo.smartgps;

import android.database.Cursor;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by CristianPC on 5/07/14.
 */
public class Dao_Coordenadas {


    private static final String TAG = "Dao Coordenadas";

    public Dao_Coordenadas() {

    }


    public boolean insertar(Cursor coordenadas, String url) {

        boolean resul = true;

        HttpClient httpClient = new DefaultHttpClient();

        String url_completa = url + "?tipo=2";
        Log.i(TAG, "Url Coo: " + url_completa);

        //pD.setProgress(+2);

        HttpPost post = new HttpPost(url_completa);
        post.setHeader("content-type", "application/json");

        try {
            // Construimos el objeto cliente en formato JSON

            JSONArray arry_datos = new JSONArray();

            if (coordenadas.moveToFirst()) {
                do {
                    Log.i(TAG, coordenadas.getString(0) + " - " + coordenadas.getString(1) + " - " + coordenadas.getString(2) + " - " + coordenadas.getString(3) + " - " + coordenadas.getInt(4));

                    JSONObject dato = new JSONObject();

                    dato.put("latitud", coordenadas.getString(0));
                    dato.put("longitud", coordenadas.getString(1));
                    dato.put("fecha", coordenadas.getString(2));
                    dato.put("hora", coordenadas.getString(3));
                    dato.put("enviado", coordenadas.getInt(4));

                    arry_datos.put(dato);

                } while (coordenadas.moveToNext());
            }


            StringEntity entity = new StringEntity(arry_datos.toString());
            post.setEntity(entity);

            HttpResponse resp = httpClient.execute(post);
            String respStr = EntityUtils.toString(resp.getEntity());


            Log.i(TAG, "esta es a respueta de Insert :" + respStr);

            //  pD.setProgress(+20);

        } catch (Exception ex) {
            Log.e("ServicioRest", "Error!", ex);

            return false;
        }

        return false;

    }

}
