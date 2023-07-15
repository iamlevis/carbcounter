package com.chrislevis.carbcounter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class CcDataSource {
    private final static String TAG = "CCDS";
    private SQLiteDatabase mCCDB;
    private CarbHistoryHelper mCHelper;
    private long mMinUid;
    private long mMaxUid;

    // TODO This should be a singleton.
    public CcDataSource(Context context) {
        mCHelper = new CarbHistoryHelper(context);
    }

    public void open() throws SQLException {
        mCCDB = mCHelper.getWritableDatabase();
        setMinMaxUids();

    }

    public void close() {
        mCHelper.close();
    }

    private void setMinMaxUids() {
        Cursor c = mCCDB.query(CarbHistoryHelper.CARB_TABLE_NAME,
                new String[]{"min(uid)", "max(uid)"},
                null, null, null, null, null);
        if (c.moveToFirst()) {
            mMinUid = c.getLong(0);
            mMaxUid = c.getLong(1);
        }
        return;
    }

    public long getMinUid() {
        return mMinUid;
    }

    public long getMaxUid() {
        return mMaxUid;
    }

    public long updateUid(long uid, ContentValues v) {
        return mCCDB.update(CarbHistoryHelper.CARB_TABLE_NAME, v, "uid=" + uid, null);
    }


    public long storeIt(CcRecord r) {
        long ret;
        ContentValues v = new ContentValues();
        //v.put(CarbHistoryHelper.F_UID, r.getUid()); // Let the DMBS do this.
        v.put(CarbHistoryHelper.F_DATE_TMS, r.getDate_tms());
        v.put(CarbHistoryHelper.F_TOTAL_CARBS, r.getTotal_carbs().toPlainString());
        v.put(CarbHistoryHelper.F_FIBER, r.getFiber().toPlainString());
        v.put(CarbHistoryHelper.F_SUGALC, r.getSugar_alc().toPlainString());
        v.put(CarbHistoryHelper.F_NET_CARBS, r.getNetcarbs().toPlainString());
        v.put(CarbHistoryHelper.F_CALORIES, r.getCalories().toPlainString());
        v.put(CarbHistoryHelper.F_COMMENTS, r.getComments());

        ret = mCCDB.insert(CarbHistoryHelper.CARB_TABLE_NAME, null, v);
        return ret;
    }

    public List<CcRecord> getAllRecords() {
        return getTopNRecords(0);
    }

    public List<CcRecord> getTopNRecords(int nrecords) {
        String thelimit = "";
        if (nrecords != 0) {
            thelimit = " limit " + nrecords;
        }

        List<CcRecord> records = new ArrayList<CcRecord>();
        
        
        /* In this query I enforce the row limit by tacking on a limit
         * statement to the order-by clause.  In a newer API level there's
         * a "query" method that actually takes a limit argument, but I
         * don't want to force a higher API just for that.
         */
        Cursor c = mCCDB.query(CarbHistoryHelper.CARB_TABLE_NAME,
                CarbHistoryHelper.CH_COLUMNS,
                null, null, null, null,
                CarbHistoryHelper.F_DATE_TMS + " desc" + thelimit);

        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                int uid = c.getInt(c.getColumnIndexOrThrow(CarbHistoryHelper.F_UID));
                String date_tms = c.getString(c.getColumnIndexOrThrow(CarbHistoryHelper.F_DATE_TMS));
                BigDecimal total_carbs = new BigDecimal(c.getFloat(c.getColumnIndexOrThrow(CarbHistoryHelper.F_TOTAL_CARBS)));
                BigDecimal fiber = new BigDecimal(c.getFloat(c.getColumnIndexOrThrow(CarbHistoryHelper.F_FIBER)));
                BigDecimal sugalc = new BigDecimal(c.getFloat(c.getColumnIndexOrThrow(CarbHistoryHelper.F_SUGALC)));
                BigDecimal net_carbs = new BigDecimal(c.getFloat(c.getColumnIndexOrThrow(CarbHistoryHelper.F_NET_CARBS)));
                BigDecimal calories = new BigDecimal(c.getFloat(c.getColumnIndexOrThrow(CarbHistoryHelper.F_CALORIES)));
                String comments = c.getString(c.getColumnIndexOrThrow(CarbHistoryHelper.F_COMMENTS));

                CcRecord thisrecord = new CcRecord(total_carbs, fiber, sugalc, calories, comments);
                thisrecord.setUid(uid);
                thisrecord.setDate_tms(date_tms);
                thisrecord.setNetcarbs(net_carbs);

                records.add(thisrecord);
                c.moveToNext();
            }
        }
        c.close();

        for (CcRecord foo : records) {
            Log.v(TAG, foo.getNetcarbs() + ": " + foo.getComments());
        }
        getTodaysStats();
        return records;
    }

    public SortedMap<Date, DailyStats> getStatsByDay() {
        // Date will be a Date, and daily stats will have
        // shortdate as a string of MM/DD, with the
        SortedMap<Date, DailyStats> h = new TreeMap<Date, DailyStats>();


        String foo =
                "select c.date as ddate " +
                        "      ,coalesce(snc,0) " +
                        "      ,coalesce(stc,0) " +
                        "      ,coalesce(sc,0) " +
                        "      ,coalesce(sf,0) " +
                        "      ,coalesce(ssa,0) " +
                        "from dates c " +
                        "  join (select date(min(date_tms)) as mindate " +
                        "              ,date(max(date_tms)) as maxdate " +
                        "        from " + CarbHistoryHelper.CARB_TABLE_NAME + " " +
                        "        where date_tms >= date(current_date,'-90 day') " +
                        "  ) b " +
                        "    on c.date between b.mindate and b.maxdate " +
                        "  left outer join (select date(date_tms) as ddate " +
                        "                         ,sum(net_carbs) as snc " +
                        "                         ,sum(total_carbs) as stc " +
                        "                         ,sum(calories) as sc " +
                        "                         ,sum(fiber) as sf " +
                        "                         ,sum(sugar_alcohol) as ssa " +
                        "                  from " + CarbHistoryHelper.CARB_TABLE_NAME + " " +
                        "                  group by 1 " +
                        "  ) d " +
                        "    on d.ddate = c.date " +
                        "group by 1 order by 1";

        Cursor c = mCCDB.rawQuery(foo, null);
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                DailyStats newstat = new DailyStats(c.getString(0),
                        new BigDecimal(c.getFloat(1)),
                        new BigDecimal(c.getFloat(2)),
                        new BigDecimal(c.getFloat(3)),
                        new BigDecimal(c.getFloat(4)),
                        new BigDecimal(c.getFloat(5))
                );
                h.put(newstat.getmDate(), newstat);
                c.moveToNext();
            }
        }

        return h;

    }


    public List<Integer> getDailyNetCarbs() {
        List<Integer> d = new ArrayList<Integer>();

        Cursor c = mCCDB.query(
                false,
                CarbHistoryHelper.CARB_TABLE_NAME,
                new String[]{"date(date_tms) as ddate", "sum(net_carbs)"},
                null, //where
                null,
                "ddate",
                null,
                "ddate",
                null
        );

        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                d.add(c.getInt(1));
                c.moveToNext();
            }
        }
        return d;
    }

    public HashMap<String, BigDecimal> getTodaysStats() {
        // Maybe learn about a hashtable here, and return
        // k,v for carbs and net carbs.
        // Next time try a Bundle.  It's hashmap-like and is
        // specific to Android and has some additional functionality.
        HashMap<String, BigDecimal> h = new HashMap<String, BigDecimal>();

        Cursor c = mCCDB.query(
                false,
                CarbHistoryHelper.CARB_TABLE_NAME,
                new String[]{"coalesce(sum(net_carbs),0)"
                        , "coalesce(sum(calories),0)"
                        , "coalesce(sum(total_carbs),0)"
                        , "coalesce(sum(fiber),0)"
                        , "coalesce(sum(sugar_alcohol),0)"
                },
                "date(date_tms)=date('now','localtime')",
                null, //if i used them, strings to replace ?s with
                null, //group by
                null, //having
                null, //order by
                null  //limit
        );

        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                h.put("daily_total_net_carbs", new BigDecimal(c.getFloat(0)));
                h.put("daily_total_calories", new BigDecimal(c.getFloat(1)));
                h.put("daily_total_carbs", new BigDecimal(c.getFloat(2)));
                h.put("daily_total_fiber", new BigDecimal(c.getFloat(3)));
                h.put("daily_total_sugalc", new BigDecimal(c.getFloat(4)));
                int tnc = c.getInt(0);
                int tc = c.getInt(1);
                Log.v(TAG, tnc + " and " + tc);
                c.moveToNext();
            }
        }
        return h;
    }

    public void deleteUid(long uid) {
        long tmp = 0;
        tmp = mCCDB.delete(CarbHistoryHelper.CARB_TABLE_NAME, "uid=?", new String[]{"" + uid});
        Log.v(TAG, "Deleted " + tmp + " records.");
    }

    public CcRecord getRecordByUid(long mUID) {
        CcRecord thisrecord = null;
        Cursor c = mCCDB.query(CarbHistoryHelper.CARB_TABLE_NAME,
                CarbHistoryHelper.CH_COLUMNS,
                CarbHistoryHelper.F_UID + "=?", new String[]{"" + mUID}, null, null,
                null);
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                int uid = c.getInt(c.getColumnIndexOrThrow(CarbHistoryHelper.F_UID));
                String date_tms = c.getString(c.getColumnIndexOrThrow(CarbHistoryHelper.F_DATE_TMS));
                BigDecimal total_carbs = new BigDecimal(c.getFloat(c.getColumnIndexOrThrow(CarbHistoryHelper.F_TOTAL_CARBS)));
                BigDecimal fiber = new BigDecimal(c.getFloat(c.getColumnIndexOrThrow(CarbHistoryHelper.F_FIBER)));
                BigDecimal sugalc = new BigDecimal(c.getFloat(c.getColumnIndexOrThrow(CarbHistoryHelper.F_SUGALC)));
                BigDecimal net_carbs = new BigDecimal(c.getFloat(c.getColumnIndexOrThrow(CarbHistoryHelper.F_NET_CARBS)));
                BigDecimal calories = new BigDecimal(c.getFloat(c.getColumnIndexOrThrow(CarbHistoryHelper.F_CALORIES)));
                String comments = c.getString(c.getColumnIndexOrThrow(CarbHistoryHelper.F_COMMENTS));

                thisrecord = new CcRecord(total_carbs, fiber, sugalc, calories, comments);
                thisrecord.setUid(uid);
                thisrecord.setDate_tms(date_tms);
                thisrecord.setNetcarbs(net_carbs);

                c.moveToNext();
            }
        }
        c.close();
        return thisrecord;
    }

    public CcRecord getRecordAfterUid(long uid) {
        //what's the right next uid? select min(uid) where uid > current one
        long uidAfter = uid;
        Cursor c = mCCDB.query(CarbHistoryHelper.CARB_TABLE_NAME,
                new String[]{"min(uid)"},
                CarbHistoryHelper.F_UID + ">?", new String[]{"" + uid}, null, null,
                null);
        if (c.moveToFirst()) {
            if (c.isNull(0)) {
                //Already at the newest record;
                uidAfter = uid;
            } else {
                uidAfter = c.getLong(0);
            }
        }
        return getRecordByUid(uidAfter);
    }

    public CcRecord getRecordBeforeUid(long uid) {
        //what's the right next uid? select max(uid) where uid < current one
        //return getRecordByUid(the right one)
        long uidBefore = uid;
        Cursor c = mCCDB.query(CarbHistoryHelper.CARB_TABLE_NAME,
                new String[]{"max(uid)"},
                CarbHistoryHelper.F_UID + "<?", new String[]{"" + uid}, null, null,
                null);
        if (c.moveToFirst()) {
            if (c.isNull(0)) {
                // Already at the oldest record.
                uidBefore = uid;
            } else {
                uidBefore = c.getLong(0);
            }
        }
        return getRecordByUid(uidBefore);
    }

    public boolean dumpToFile(String p) {
        // Given a path, dump to it.
        try {
            File f = new File(p);
            BufferedWriter buf = new BufferedWriter(new FileWriter(f));
            buf.append("Date_Time,Total_carbs,Fiber,Sugar_alcohols,Net_carbs,Calories,Comments");
            buf.newLine();
            for (CcRecord thisRecord : getTopNRecords(0)) {
                buf.append(thisRecord.getDate_tms() + ",");
                buf.append(thisRecord.getTotal_carbs() + ",");
                buf.append(thisRecord.getFiber() + ",");
                buf.append(thisRecord.getSugar_alc() + ",");
                buf.append(thisRecord.getNetcarbs() + ",");
                buf.append(thisRecord.getCalories() + ",");
                buf.append(thisRecord.getComments());
                buf.newLine();
            }
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            Log.v(TAG, "File not found writing CSV.");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.v(TAG, "I/O error writing CSV.");
            e.printStackTrace();
            return false;
        }

        return true;
    }


}
