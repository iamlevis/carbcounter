package com.chrislevis.carbcounter;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

//import com.google.ads.AdRequest;
//import com.google.ads.AdView;
//import android.util.Log;


public class MainActivity extends ListActivity {
    public final static String VAL_CALS = "com.chrislevis.carbcounter.VAL_CALS";
    public final static String VAL_TCARBS = "com.chrislevis.carbcounter.VAL_TCARBS";
    public final static String VAL_FIBER = "com.chrislevis.carbcounter.VAL_FIBER";
    public final static String VAL_SUGARALC = "com.chrislevis.carbcounter.VAL_SUGARALC";
    private final static String TAG = "CJL_MAIN";
    public SharedPreferences mPreferences;
    private Context mContext = null;
    private CcDataSource mCCDS;
    private Integer mScale = 0;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getWindow().getContext();

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mScale = Integer.parseInt(mPreferences.getString("display_precision", "0"));

        //TODO the style is to name members mMyVar, instead of using "this."
        this.mCCDS = new CcDataSource(this);
        this.mCCDS.open();
        this.refreshHistoryView();
        //TODO Find out where to do the datasource close.

        MobileAds.initialize(this, "ca-app-pub-4339702950459490~7344576645");
        mAdView = (AdView) findViewById(R.id.adViewMainActivity);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 00);
        calendar.set(Calendar.MINUTE, 00);
        calendar.set(Calendar.SECOND, 00);

        Intent intent = new Intent(this, WidgetUpdateService.class);
        PendingIntent pintent = PendingIntent.getService(this, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pintent);

        // After some time, ask people to rate.
        AppRater.app_launched(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent updateWidget = new Intent(mContext, CcWidgetProvider.class);
        updateWidget.setAction("update_widget");
        PendingIntent pending = PendingIntent.getBroadcast(mContext, 0, updateWidget, PendingIntent.FLAG_CANCEL_CURRENT);
        try {
            pending.send();
        } catch (CanceledException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void refreshHistoryView() {
        // Show net so far today.
        HashMap<String, BigDecimal> dstats = mCCDS.getTodaysStats();
        setTitle("CarbCounter: " + dstats.get("daily_total_net_carbs").setScale(mScale, BigDecimal.ROUND_HALF_EVEN) + " net carbs.");

        // Show the list of all of them.
        List<CcRecord> values = mCCDS.getTopNRecords(100);

        //Quelle hack.
        for (CcRecord c : values) {
            c.setNumDecPlaces(mScale);
        }

        ArrayAdapter<CcRecord> adapter = new ArrayAdapter<CcRecord>(this, android.R.layout.simple_list_item_1, values);
        setListAdapter(adapter);

        ListView thelv = (ListView) findViewById(android.R.id.list);
        thelv.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final long theUid = ((CcRecord) getListAdapter().getItem(position)).getUid();
                final CcRecord thisRecord = ((CcRecord) getListAdapter().getItem(position));
                final String theComment = thisRecord.getComments();

                final AlertDialog.Builder b = new AlertDialog.Builder(mContext);

                if (theComment == "") {
                    b.setTitle("Actions for this item.");
                } else {
                    b.setTitle("Actions for \"" + theComment + "\".");
                }

                b.setItems(new CharSequence[]{"View/edit details", "Delete entry"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog2, int item) {
                                if (item == 0) {
                                    // Learn how to just pass the whole object, if possible.
                                    Intent detview = new Intent(mContext, DetailsView.class);
                                    detview.putExtra("com.chrislevis.CarbCounter.uid", thisRecord.getUid());
                                    startActivity(detview);
                                    return;
                                }

                                if (item == 1) {
                                    mCCDS.deleteUid(theUid);
                                    Toast.makeText(MainActivity.this, "Deleted!", Toast.LENGTH_LONG).show();
                                    refreshHistoryView();
                                    return;
                                }
                            }
                        }
                );

                b.create();
                b.show();

                return true;

            }
        });
    }

    public void doDeleteNewest(View view) {
        CcRecord r = null;
        if (getListAdapter().getCount() > 0) {
            r = (CcRecord) getListAdapter().getItem(0);
            mCCDS.deleteUid(r.getUid());
            refreshHistoryView();
        }
    }

    public void doRecordThisClick(View view) {
        Log.i(TAG, "Ok, so i'm recording it.");
        BigDecimal tcals;
        BigDecimal tcarbs;
        BigDecimal fiber;
        BigDecimal sugalc;
        StringBuilder description = new StringBuilder("");

        EditText t;

        try {
            t = (EditText) findViewById(R.id.input_total_calories);
            tcals = new BigDecimal(t.getText().toString()).setScale(mScale, BigDecimal.ROUND_HALF_EVEN);
        } catch (NumberFormatException e) {
            // okay to be missing
            tcals = new BigDecimal(0);
        }

        try {
            t = (EditText) findViewById(R.id.input_total_carbs);
            tcarbs = new BigDecimal(t.getText().toString()).setScale(mScale, BigDecimal.ROUND_HALF_EVEN);
        } catch (NumberFormatException e) {
            // TODO: MUST NOT be missing
            tcarbs = new BigDecimal(0);
        }

        try {
            t = (EditText) findViewById(R.id.input_fiber);
            fiber = new BigDecimal(t.getText().toString()).setScale(mScale, BigDecimal.ROUND_HALF_EVEN);
        } catch (NumberFormatException e) {
            // okay to be missing
            fiber = new BigDecimal(0);
        }

        try {
            t = (EditText) findViewById(R.id.input_sugar_alcohol);
            sugalc = new BigDecimal(t.getText().toString()).setScale(mScale, BigDecimal.ROUND_HALF_EVEN);
        } catch (NumberFormatException e) {
            // okay to be missing
            sugalc = new BigDecimal(0);
        }


        t = (EditText) findViewById(R.id.input_description);
        description.append(t.getText().toString());

        StringBuilder sb = new StringBuilder();
        ///////////////
        BigDecimal tmp = tcarbs.subtract(fiber).subtract(sugalc).setScale(1, BigDecimal.ROUND_HALF_EVEN);
        sb = sb.append(tcals).append(" calories and ").append(tmp.toPlainString()).append(" net carbs");
        //Log.i(TAG,description.toString());


        CcRecord r = new CcRecord(tcarbs, fiber, sugalc, tcals, t.getText().toString());
        CcDataSource c = new CcDataSource(getBaseContext());
        c.open();
        long row_id;
        row_id = c.storeIt(r);
        Log.v(TAG, "row_id=" + row_id);

        // TODO learn how to do this with an array.
        if (row_id > 0) {
            ((EditText) findViewById(R.id.input_total_calories)).setText("");
            ((EditText) findViewById(R.id.input_total_carbs)).setText("");
            ((EditText) findViewById(R.id.input_fiber)).setText("");
            ((EditText) findViewById(R.id.input_sugar_alcohol)).setText("");
            ((EditText) findViewById(R.id.input_description)).setText("");
            Toast.makeText(MainActivity.this, "Saved.", Toast.LENGTH_LONG).show();

        }
        c.close();

        // Couple hoops to dismiss the keyboard.
        findViewById(R.id.input_total_carbs).requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(R.id.input_total_carbs).getWindowToken(), 0);
        findViewById(R.id.input_total_carbs).clearFocus();

        this.refreshHistoryView();
        return;
    }

    public void doShowChartsClick(View view) {
        Intent intent = new Intent(this, ShowCharts.class);
        startActivity(intent);
        return;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // Launch Preference activity
                Intent i = new Intent(MainActivity.this, CcPreferences.class);
                startActivity(i);
                break;
            case R.id.action_emailcsv:
                // Launch Preference activity
                sendHistToEmail(mContext);
                break;
        }
        return true;
    }

    public void sendHistToEmail(Context context) {
        /* Dump to file, and get that file's path. */
        final String dpath = dumpHistToFile(context);
        
        
        /* Prompt for target email address. */
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new AlertDialog.Builder(mContext)
                .setTitle("Send history")
                .setMessage("Enter an email address.")
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        sendEmailWithAttachment(mContext, input.getText().toString(),
                                "CarbCounter history attached.", dpath);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
        // see http://androidsnippets.com/prompt-user-input-with-an-alertdialog


    }

    private boolean sendEmailWithAttachment(Context context, String emTo, String emMsg, String attPath) {
        Log.v(TAG, "Sending " + emMsg + " to " + emTo + " with " + attPath);

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("plain/text");
        i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(attPath)));
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{emTo});
        i.putExtra(Intent.EXTRA_SUBJECT, "CarbCounter history");
        i.putExtra(Intent.EXTRA_TEXT, "CarbCounter history is attached.\n\nRegards,\nOof Studios\n");
        startActivity(Intent.createChooser(i, "Email"));
        return true;
    }

    private String dumpHistToFile(Context context) {
        // Create a file with the history dump.
        // Returns the full path to that file.
        String dt = (new SimpleDateFormat("yyyyMMdd", Locale.US)).format(new Date());
        String fname = "CarbCounter_" + dt + ".csv";

        StringBuilder sb = new StringBuilder(Environment.getExternalStorageDirectory() + File.separator + fname);
        Log.v(TAG, sb.toString());
        if (mCCDS.dumpToFile(sb.toString())) {
            //Ready to email fname.
            return sb.toString();
        } else {
            Toast.makeText(MainActivity.this, "An error occurred.  Please contact me.", Toast.LENGTH_LONG).show();
        }
        return "";

    }


}
