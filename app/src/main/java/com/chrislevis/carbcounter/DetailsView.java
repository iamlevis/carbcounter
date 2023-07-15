package com.chrislevis.carbcounter;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.math.BigDecimal;


public class DetailsView extends Activity {
    private static final String TAG = "DETAILS";
    private CcDataSource mCCDS;
    private Context mContext;
    private long mCurrentUid;
    private boolean mInEditMode = false;
    private Integer mScale = 0;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_view);
        // Show the Up button in the action bar.
        setupActionBar();

        mContext = getWindow().getContext();
        mAdView = (AdView) findViewById(R.id.adView_Details);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mScale = Integer.parseInt(prefs.getString("display_precision", "0"));

        mCurrentUid = getIntent().getLongExtra("com.chrislevis.CarbCounter.uid", 0);
        mCCDS = new CcDataSource(this);
        mCCDS.open();
        CcRecord theRecord = mCCDS.getRecordByUid(mCurrentUid);
        theRecord.setNumDecPlaces(mScale);
        fillInDetails(theRecord);

    }

    private long getCurrentUid() {
        return mCurrentUid;
    }

    private void setCurrentUid(long uid) {
        mCurrentUid = uid;
    }

    public void onNextRecord(View v) {
        CcRecord theRecord = mCCDS.getRecordAfterUid(getCurrentUid());
        setCurrentUid(theRecord.getUid());
        fillInDetails(theRecord);
    }

    public void onPreviousRecord(View v) {
        CcRecord theRecord = mCCDS.getRecordBeforeUid(getCurrentUid());
        setCurrentUid(theRecord.getUid());
        fillInDetails(theRecord);
    }

    private void setupActionBar() {
        //TODO I don't think this is technically needed.  It's the only
        //     thing keeping me at API 11.  I could get a bigger audience
        //     without it...
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.details_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private long commitChanges() {

        BigDecimal total_carbs = new BigDecimal(0);
        BigDecimal fiber = new BigDecimal(0);
        BigDecimal sugalc = new BigDecimal(0);
        BigDecimal calories = new BigDecimal(0);
        String comments = ((EditText) findViewById(R.id.ed_input_description)).getText().toString();

        EditText t;
        try {
            t = (EditText) findViewById(R.id.ed_input_total_carbs);
            total_carbs = new BigDecimal(t.getText().toString()).setScale(mScale, BigDecimal.ROUND_HALF_EVEN);
        } catch (NumberFormatException e) {
            // okay to be missing
            total_carbs = new BigDecimal(0);
        }

        try {
            t = (EditText) findViewById(R.id.ed_input_total_calories);
            calories = new BigDecimal(t.getText().toString()).setScale(mScale, BigDecimal.ROUND_HALF_EVEN);
        } catch (NumberFormatException e) {
            // okay to be missing
            calories = new BigDecimal(0);
        }

        try {
            t = (EditText) findViewById(R.id.ed_input_fiber);
            fiber = new BigDecimal(t.getText().toString()).setScale(mScale, BigDecimal.ROUND_HALF_EVEN);
        } catch (NumberFormatException e) {
            // okay to be missing
            fiber = new BigDecimal(0);
        }

        try {
            t = (EditText) findViewById(R.id.ed_input_sugar_alcohol);
            sugalc = new BigDecimal(t.getText().toString()).setScale(mScale, BigDecimal.ROUND_HALF_EVEN);
        } catch (NumberFormatException e) {
            // okay to be missing
            sugalc = new BigDecimal(0);
        }

        BigDecimal net_carbs = total_carbs.subtract(fiber).subtract(sugalc);
        ContentValues u = new ContentValues();
        //Make a content-values so I don't have to escape comments.
        u.put(CarbHistoryHelper.F_TOTAL_CARBS, total_carbs.toPlainString());
        u.put(CarbHistoryHelper.F_FIBER, fiber.toPlainString());
        u.put(CarbHistoryHelper.F_SUGALC, sugalc.toPlainString());
        u.put(CarbHistoryHelper.F_NET_CARBS, net_carbs.toPlainString());
        u.put(CarbHistoryHelper.F_CALORIES, calories.toPlainString());
        u.put(CarbHistoryHelper.F_COMMENTS, comments);

        //The timestamp is fixed, but net carbs may have changed:
        ((TextView) findViewById(R.id.dtl_netcarbs)).setText(net_carbs + "");

        findViewById(R.id.ed_input_total_carbs).requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(R.id.ed_input_total_carbs).getWindowToken(), 0);
        findViewById(R.id.ed_input_total_carbs).clearFocus();

        return mCCDS.updateUid(mCurrentUid, u);

    }

    public void onSaveEdit(View view) {
        if (!mInEditMode) {
            mInEditMode = true;
            ((EditText) findViewById(R.id.ed_input_total_carbs)).setFocusable(true);
            ((EditText) findViewById(R.id.ed_input_total_carbs)).setFocusableInTouchMode(true);
            ((EditText) findViewById(R.id.ed_input_total_carbs)).requestFocus();

            ((EditText) findViewById(R.id.ed_input_total_calories)).setFocusable(true);
            ((EditText) findViewById(R.id.ed_input_total_calories)).setFocusableInTouchMode(true);

            ((EditText) findViewById(R.id.ed_input_fiber)).setFocusable(true);
            ((EditText) findViewById(R.id.ed_input_fiber)).setFocusableInTouchMode(true);

            ((EditText) findViewById(R.id.ed_input_sugar_alcohol)).setFocusable(true);
            ((EditText) findViewById(R.id.ed_input_sugar_alcohol)).setFocusableInTouchMode(true);

            ((EditText) findViewById(R.id.ed_input_description)).setFocusable(true);
            ((EditText) findViewById(R.id.ed_input_description)).setFocusableInTouchMode(true);

            ((Button) findViewById(R.id.dtl_previous)).setEnabled(false);
            ((Button) findViewById(R.id.dtl_next)).setEnabled(false);
            ((Button) findViewById(R.id.saveAndEdit)).setText("Save");
        } else if (mInEditMode) {
            mInEditMode = false;
            ((EditText) findViewById(R.id.ed_input_total_carbs)).setFocusable(false);
            ((EditText) findViewById(R.id.ed_input_total_calories)).setFocusable(false);
            ((EditText) findViewById(R.id.ed_input_fiber)).setFocusable(false);
            ((EditText) findViewById(R.id.ed_input_sugar_alcohol)).setFocusable(false);
            ((EditText) findViewById(R.id.ed_input_description)).setFocusable(false);

            ((Button) findViewById(R.id.dtl_previous)).setEnabled(true);
            ((Button) findViewById(R.id.dtl_next)).setEnabled(true);
            ((Button) findViewById(R.id.saveAndEdit)).setText("Edit");

            if (commitChanges() > 0) {
                Toast.makeText(mContext, "Saved.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, "Couldn't save.  Sorry :(", Toast.LENGTH_LONG).show();
            }
        }
        return;
    }

    private void fillInDetails(CcRecord theRecord) {
        // If I'm at the edge of the observable universe, a loving God
        // will prevent me from stepping off.
        if (getCurrentUid() == mCCDS.getMaxUid()) {
            ((Button) findViewById(R.id.dtl_next)).setEnabled(false);
        } else {
            ((Button) findViewById(R.id.dtl_next)).setEnabled(true);
        }
        if (getCurrentUid() == mCCDS.getMinUid()) {
            ((Button) findViewById(R.id.dtl_previous)).setEnabled(false);
        } else {
            ((Button) findViewById(R.id.dtl_previous)).setEnabled(true);
        }

        EditText t = null;

        // Date
        ((TextView) findViewById(R.id.dtl_datetms)).setText(theRecord.getDate_tms() + "");

        // Net carbs
        ((TextView) findViewById(R.id.dtl_netcarbs)).setText(theRecord.getNetcarbs() + "");

        // Total carbs
        t = (EditText) findViewById(R.id.ed_input_total_carbs);
        t.setText(theRecord.getTotal_carbs() + "");
        t.setFocusable(false);

        // Calories, or not.
        t = (EditText) findViewById(R.id.ed_input_total_calories);
        t.setText(theRecord.getCalories() + "");
        t.setFocusable(false);

        // Fiber
        t = (EditText) findViewById(R.id.ed_input_fiber);
        t.setText(theRecord.getFiber() + "");
        t.setFocusable(false);

        // Sugar alcohol
        t = (EditText) findViewById(R.id.ed_input_sugar_alcohol);
        t.setText(theRecord.getSugar_alc() + "");
        t.setFocusable(false);

        // Comments
        t = (EditText) findViewById(R.id.ed_input_description);
        t.setText(theRecord.getComments() + "");
        t.setFocusable(false);

    }
}
