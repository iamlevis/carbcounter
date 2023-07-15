package com.chrislevis.carbcounter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;


public class ShowCharts extends Activity {
    private CcDataSource mCCDS;
    private Context mContext;
    private GraphView mTheGraph;
    private AdView mAdView;

    private Integer mMetricMax = 0;
    private Integer mMetricMin = 0;
    private ArrayList<String> mYLabels;
    private ArrayList<String> mXLabels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_charts);
        // Show the Up button in the action bar.
        setupActionBar();

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mContext = getWindow().getContext();

        mCCDS = new CcDataSource(this);
        mCCDS.open();
        refreshCharts();
    }

    private GraphViewSeries getGvSeriesForMetric(Integer metric, SortedMap<Date, DailyStats> day_stats) {
        // Also makes Y-axis lables as a side-effect.
        mYLabels = new ArrayList<String>();
        mXLabels = new ArrayList<String>();
        mMetricMax = 0;

        List<GraphViewData> metric_data = new ArrayList<GraphViewData>();

        int i = 0;
        //int i_end = day_stats.l
        for (Map.Entry<Date, DailyStats> thisTm : day_stats.entrySet()) {
            DailyStats thestats = thisTm.getValue();
            Integer the_metric = 0;
            if (metric == R.id.rb_netcarbs) {
                the_metric = thestats.getDaily_net_carbs().intValue();
            } else if (metric == R.id.rb_carbs) {
                the_metric = thestats.getDaily_carbs().intValue();
            } else if (metric == R.id.rb_calories) {
                the_metric = thestats.getDaily_calories().intValue();
            } else if (metric == R.id.rb_fiber) {
                the_metric = thestats.getDaily_fiber().intValue();
            } else if (metric == R.id.rb_sugaralc) {
                the_metric = thestats.getDaily_sugalc().intValue();
            }

            metric_data.add(new GraphViewData(i, the_metric));
            if (thisTm.getKey() == day_stats.firstKey()
                    || thisTm.getKey() == day_stats.lastKey()) {
                mXLabels.add(thestats.getShortDateText());
            } else {
                mXLabels.add("");
            }

            mMetricMax = the_metric > mMetricMax ? the_metric : mMetricMax;
            i += 1;
        }
        
        /* Set Y-axis labels. */
        if (((mMetricMax - mMetricMin) / 8) < 1) {
            mYLabels.add("" + mMetricMin);
            mYLabels.add("" + (mMetricMax + 1));
        } else {
            int y = 0;
            do {
                mYLabels.add("" + y);
                y += (mMetricMax - mMetricMin) / 8;
            } while (y < mMetricMax * 1.25);
        }
        Collections.reverse(mYLabels);

        return new GraphViewSeries(metric_data.toArray(new GraphViewData[0]));
    }

    private void refreshCharts() {
        SortedMap<Date, DailyStats> tm = mCCDS.getStatsByDay();

        GraphView gv = null;
        if (((RadioButton) findViewById(R.id.graph_style_bar)).isChecked()) {
            gv = new BarGraphView(this, "");
        } else if (((RadioButton) findViewById(R.id.graph_style_line)).isChecked()) {
            gv = new LineGraphView(this, "");
        } else {
            gv = new BarGraphView(this, "");
        }

        GraphViewSeries theSeries = null;
        RadioGroup rg = (RadioGroup) findViewById(R.id.rbg_Metric);
        gv.setTitle("Daily " + ((RadioButton) findViewById(rg.getCheckedRadioButtonId())).getText().toString());

        theSeries = getGvSeriesForMetric(rg.getCheckedRadioButtonId(), tm);

        gv.addSeries(theSeries);
        gv.setHorizontalLabels(mXLabels.toArray(new String[0]));
        //gv.setHorizontalLabels(new String[]{});
        gv.setVerticalLabels(mYLabels.toArray(new String[mYLabels.size()]));
        gv.setManualYAxisBounds(Math.round(mMetricMax * 1.25), 0);
        //gv.setViewPort(1, 10);
        //gv.setScrollable(true);
        //gv.scrollToEnd();

        LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
        layout.removeAllViews(); //Clear out the old chart.
        layout.addView(gv);

        mTheGraph = gv;

    }


    public void onStyleChange(View view) {
        refreshCharts();
    }

    public void onChangeChart(View view) {
        refreshCharts();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
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

}
