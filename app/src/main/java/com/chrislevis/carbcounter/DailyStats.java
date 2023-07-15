package com.chrislevis.carbcounter;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class DailyStats {
    private Date mDate;
    private BigDecimal daily_net_carbs;
    private BigDecimal daily_calories;
    private BigDecimal daily_fiber;
    private BigDecimal daily_carbs;
    private BigDecimal daily_sugalc;

    private SimpleDateFormat date_in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private SimpleDateFormat date_out = new SimpleDateFormat("MM/dd", Locale.US);

    public DailyStats(String date_tms,
                      BigDecimal net_carbs,
                      BigDecimal total_carbs,
                      BigDecimal calories,
                      BigDecimal fiber,
                      BigDecimal sugar_alcohol) {
        setmDate(date_tms);
        setDaily_net_carbs(net_carbs);
        setDaily_carbs(total_carbs);
        setDaily_calories(calories);
        setDaily_fiber(fiber);
        setDaily_sugalc(sugar_alcohol);
    }

    public Date getmDate() {
        return mDate;
    }

    public void setmDate(Date mDate) {
        this.mDate = mDate;
    }

    public String getShortDateText() {
        return date_out.format(mDate);
    }

    public void setmDate(String mDateText) {
        try {
            mDate = date_in.parse(mDateText);
        } catch (ParseException e) {
            // Don't try to fix bad dates from SQL.
            ;
        }
    }

    public BigDecimal getDaily_net_carbs() {
        return daily_net_carbs;
    }

    public void setDaily_net_carbs(BigDecimal daily_net_carbs) {
        this.daily_net_carbs = daily_net_carbs;
    }

    public BigDecimal getDaily_calories() {
        return daily_calories;
    }

    public void setDaily_calories(BigDecimal daily_calories) {
        this.daily_calories = daily_calories;
    }

    public BigDecimal getDaily_fiber() {
        return daily_fiber;
    }

    public void setDaily_fiber(BigDecimal daily_fiber) {
        this.daily_fiber = daily_fiber;
    }

    public BigDecimal getDaily_carbs() {
        return daily_carbs;
    }

    public void setDaily_carbs(BigDecimal daily_carbs) {
        this.daily_carbs = daily_carbs;
    }

    public BigDecimal getDaily_sugalc() {
        return daily_sugalc;
    }

    public void setDaily_sugalc(BigDecimal daily_sugalc) {
        this.daily_sugalc = daily_sugalc;
    }


}
