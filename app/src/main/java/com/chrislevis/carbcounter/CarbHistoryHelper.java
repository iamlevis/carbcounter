package com.chrislevis.carbcounter;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CarbHistoryHelper extends SQLiteOpenHelper {

    public static final String CARB_TABLE_NAME = "carb_hist";
    public static final String CARB_DB_NAME = "carb_counter.db";
    public static final int DATABASE_VERSION = 2;

    public static final String F_UID = "uid";
    public static final String F_DATE_TMS = "date_tms";
    public static final String F_TOTAL_CARBS = "total_carbs";
    public static final String F_FIBER = "fiber";
    public static final String F_SUGALC = "sugar_alcohol";
    public static final String F_NET_CARBS = "net_carbs";
    public static final String F_CALORIES = "calories";
    public static final String F_COMMENTS = "comments";

    public static final String[] CH_COLUMNS = {
            F_UID, F_DATE_TMS, F_TOTAL_CARBS, F_FIBER, F_SUGALC,
            F_NET_CARBS, F_CALORIES, F_COMMENTS
    };


    public CarbHistoryHelper(Context context) {
        super(context, CARB_DB_NAME, null, DATABASE_VERSION);
        return;
    }

    public void createCalendar(SQLiteDatabase database) {
        database.execSQL("create table dates (id integer primary key);");
        database.execSQL("insert into dates default values;");
        database.execSQL("insert into dates default values;");
        database.execSQL("insert into dates select null from dates d1, dates d2, dates d3 , dates d4;");
        database.execSQL("insert into dates select null from dates d1, dates d2, dates d3 , dates d4;");
        database.execSQL("alter table dates add date datetime;");
        database.execSQL("update dates set date=date('2000-01-01',(-1+id)||' day');");
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        createCalendar(database);
        String Q_CREATE_CARB_HIST_TABLE =
                "create table " + CARB_TABLE_NAME + "("
                        + F_UID + " integer primary key autoincrement,"
                        + F_DATE_TMS + " datetime,"
                        + F_TOTAL_CARBS + " integer,"
                        + F_FIBER + " integer,"
                        + F_SUGALC + " integer,"
                        + F_NET_CARBS + " integer,"
                        + F_CALORIES + " integer,"
                        + F_COMMENTS + " varchar(255)"
                        + ");";

        database.execSQL(Q_CREATE_CARB_HIST_TABLE);
        String ts = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)).format(new Date());
        ContentValues v = new ContentValues();
        v.put(CarbHistoryHelper.F_DATE_TMS, ts);
        v.put(CarbHistoryHelper.F_TOTAL_CARBS, "0");
        v.put(CarbHistoryHelper.F_FIBER, "0");
        v.put(CarbHistoryHelper.F_SUGALC, "0");
        v.put(CarbHistoryHelper.F_NET_CARBS, "0");
        v.put(CarbHistoryHelper.F_CALORIES, "0");
        v.put(CarbHistoryHelper.F_COMMENTS, "New diary!");

        database.insert(CarbHistoryHelper.CARB_TABLE_NAME, null, v);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int upgradingFrom, int upgradingTo) {
        Log.w(CarbHistoryHelper.class.getName(),
                "Upgrading database from version " + upgradingFrom + " to "
                        + upgradingTo + ".");
        if (1 == upgradingFrom) {
            createCalendar(db);
        }
        //db.execSQL("DROP TABLE IF EXISTS " + CARB_TABLE_NAME);
        //onCreate(db);
    }

}
