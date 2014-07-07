package com.smartinfo.smartgps;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Httppostaux {

    private static final String TAG = "HTTTstatus";
    InputStream is = null;
    String result = "";

    public JSONArray getserverdata(ArrayList<NameValuePair> parameters, String urlwebserver, String tiempo_envio, String distancia_envio) {

        Log.i(TAG, "llamado " + parameters);
        httppostconnect(parameters, urlwebserver, tiempo_envio, distancia_envio);

        if (is != null) {
            getpostresponse();
            return getjsonarray();

        } else {
            return null;
        }

    }

    private JSONArray getjsonarray() {
        // TODO Auto-generated method stub
        try {
            JSONArray jArray = new JSONArray(result);
            return jArray;

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing data" + e.toString());
            return null;
        }
    }

    public void getpostresponse() {
        // TODO Auto-generated method stub
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            result = sb.toString();
            Log.i(TAG, "getpostresponse  result= " + sb.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error converting result " + e.toString());
        }

    }

    public void httppostconnect(ArrayList<NameValuePair> parameters, String urlwebserver, String tiempo_envio, String distancia_envio) {
        // TODO Auto-generated method stub
        try {

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(urlwebserver);
            httppost.setEntity(new UrlEncodedFormEntity(parameters));

            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            Log.i(TAG, "HTML URL= " + urlwebserver);
            Log.i(TAG, "TIEMPO TIME= " + tiempo_envio);
            Log.i(TAG, "DISTANCIA MTS= " + distancia_envio);
             is = entity.getContent();
        } catch (Exception e) {
            Log.e(TAG, "Error in http connection " + e.toString());
        }

    }

}