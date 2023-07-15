package com.chrislevis.carbcounter;


import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class WidgetUpdateService extends Service {


    public int onStartCommand(Intent intent, int flags, int startId) {
        //Anything you put here is run at the time you set the alarm to
        Log.v("WUS", "shezhooled");

        Context context = this.getApplicationContext();

        //AppWidgetManager mgr = AppWidgetManager.getInstance(context);

        Intent updateWidget = new Intent(context, CcWidgetProvider.class);
        updateWidget.setAction("update_widget");
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, updateWidget, PendingIntent.FLAG_CANCEL_CURRENT);
        try {
            pending.send();
        } catch (CanceledException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return 0;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

}

/*
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class WidgetUpdateService extends IntentService {
    private static String TAG = "WUS";
    
    public WidgetUpdateService(String name) {
        super(name);
        Log.v(TAG,"ctor");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG,"onHandleIntent");

    }

}
*/