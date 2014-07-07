package com.smartinfo.smartgps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReceiverBoot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // LANZAR SERVICIO
        Intent serviceIntent = new Intent(); /* a�adi los parametros (content, smartGPS.class)*/
        serviceIntent.setAction("com.example.smartgps.ServiceBoot");
        context.startService(serviceIntent);

	/*	// LANZAR ACTIVIDAD
        Intent i = new Intent(context, SmartGPS.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i); */
    }
}
