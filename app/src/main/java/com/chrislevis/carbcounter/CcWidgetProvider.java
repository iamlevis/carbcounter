package com.chrislevis.carbcounter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.math.BigDecimal;
import java.util.HashMap;


public class CcWidgetProvider extends AppWidgetProvider {
    public static final String TAG = "CCW";
    private static CcDataSource mCCDS;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "CcWidgetProvider onReceive()");
        if ((intent.getAction().equals("update_widget")) || (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))) {
            //Date d = new Date();
            //String showme = d.toString();

            // Manual or automatic widget update started
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.cc_widget_layout);

            // Update text, images, whatever - here
            remoteViews.setTextViewText(R.id.cc_widget_value, getWidgetMessage(context));

            // Trigger widget layout update
            AppWidgetManager awm = AppWidgetManager.getInstance(context);
            awm.updateAppWidget(new ComponentName(context, CcWidgetProvider.class), remoteViews);

            Intent launchCc = new Intent(context, MainActivity.class);
            PendingIntent launchPendingCc = PendingIntent.getActivity(context, 0, launchCc, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.cc_widget, launchPendingCc);
            ComponentName ccWidget = new ComponentName(context, CcWidgetProvider.class);
            awm.updateAppWidget(ccWidget, remoteViews);

        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] widgetIds) {
        Log.v(TAG, "CcWidgetProvider onUpdate()");
        try {
            updateWidgetContent(context, mgr);
        } catch (Exception d) {
            Log.v(TAG, "Effer");
        }
    }

    public void updateWidgetContent(Context context, AppWidgetManager mgr) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.cc_widget_layout);
        rv.setTextViewText(R.id.cc_widget_value, getWidgetMessage(context));

        Intent launchCc = new Intent(context, MainActivity.class);
        PendingIntent launchPendingCc = PendingIntent.getActivity(context, 0, launchCc, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.cc_widget, launchPendingCc);
        ComponentName ccWidget = new ComponentName(context, CcWidgetProvider.class);
        mgr.updateAppWidget(ccWidget, rv);
    }

    private String getWidgetMessage(Context context) {
        //Date d = new Date();
        //String blerg = d.toString();
        mCCDS = new CcDataSource(context);
        mCCDS.open();
        HashMap<String, BigDecimal> dstats = mCCDS.getTodaysStats();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Integer bdscale = Integer.parseInt(prefs.getString("display_precision", "0"));

        StringBuilder showme = new StringBuilder("");
        if (prefs.getBoolean("show_netcarbs", true))
            showme.append(dstats.get("daily_total_net_carbs").setScale(bdscale, BigDecimal.ROUND_HALF_EVEN) + "g net carbs\n");
        if (prefs.getBoolean("show_calories", true))
            showme.append(dstats.get("daily_total_calories").setScale(bdscale, BigDecimal.ROUND_HALF_EVEN) + " calories\n");
        if (prefs.getBoolean("show_carbs", true))
            showme.append(dstats.get("daily_total_carbs").setScale(bdscale, BigDecimal.ROUND_HALF_EVEN) + "g carbs\n");
        if (prefs.getBoolean("show_fiber", true))
            showme.append(dstats.get("daily_total_fiber").setScale(bdscale, BigDecimal.ROUND_HALF_EVEN) + "g fiber\n");
        if (prefs.getBoolean("show_sugalc", true))
            showme.append(dstats.get("daily_total_sugalc").setScale(bdscale, BigDecimal.ROUND_HALF_EVEN) + "g sug. alc.\n");
        //showme.append(blerg);

        return showme.toString();
    }


}





