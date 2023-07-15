package com.chrislevis.carbcounter;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//TODO: What if this just extends Application?  Do I get access to prefs and stuff? 
public class CcRecord {

    private long uid = 0;
    private String date_tms = "";
    private BigDecimal total_carbs = new BigDecimal(0);
    private BigDecimal fiber = new BigDecimal(0);
    private BigDecimal sugar_alc = new BigDecimal(0);
    private BigDecimal calories = new BigDecimal(0);
    private BigDecimal netcarbs = new BigDecimal(0);
    private String comments = "";
    private Integer mDecPlaces = 1;

    public CcRecord(BigDecimal tcarbs, BigDecimal fiber, BigDecimal sugalc, BigDecimal tcals, String comments) {
        setUid(0);
        setDate_tms(new Date());
        setTotal_carbs(tcarbs);
        setFiber(fiber);
        setSugar_alc(sugalc);
        setCalories(tcals);
        setComments(comments);
        setNetcarbs(tcarbs.subtract(getFiber()).subtract(getSugar_alc()));
    }

    public String toString() {
        SimpleDateFormat date_in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        SimpleDateFormat date_out = new SimpleDateFormat("MM/dd HH:mm", Locale.US);

        Date tmDate = new Date();
        String shortDate = "";
        try {
            tmDate = date_in.parse(this.getDate_tms());
        } catch (ParseException e) {
            //Pass: can't do much if the date is stored wrong in sqlite.
            ;
        }
        shortDate = date_out.format(tmDate);

        String repr = null;
        repr = shortDate + "> Net carbs: " + this.getNetcarbs();
        if (this.getComments().length() > 1) {
            repr += "\n" + this.getComments();
        }
        return repr;
    }

    public BigDecimal getNetcarbs() {
        netcarbs = netcarbs.setScale(mDecPlaces, BigDecimal.ROUND_HALF_EVEN);
        return netcarbs;
    }

    public void setNetcarbs(BigDecimal netcarbs) {
        if (netcarbs.floatValue() < 0) {
            this.netcarbs = new BigDecimal(0);
        } else {
            this.netcarbs = netcarbs;
        }
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getDate_tms() {
        return date_tms;
    }

    public void setDate_tms(String date_tms) {
        this.date_tms = date_tms;
    }

    public void setDate_tms(Date now) {
        //this.date_tms = date_tms;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        this.date_tms = f.format(now);
    }

    public BigDecimal getTotal_carbs() {
        total_carbs = total_carbs.setScale(mDecPlaces, BigDecimal.ROUND_HALF_EVEN);
        return total_carbs;
    }

    public void setTotal_carbs(BigDecimal tcarbs) {
        this.total_carbs = tcarbs;
    }

    public BigDecimal getFiber() {
        fiber = fiber.setScale(mDecPlaces, BigDecimal.ROUND_HALF_EVEN);
        return fiber;
    }

    public void setFiber(BigDecimal fiber) {
        this.fiber = fiber;
    }

    public BigDecimal getSugar_alc() {
        sugar_alc = sugar_alc.setScale(mDecPlaces, BigDecimal.ROUND_HALF_EVEN);
        return sugar_alc;
    }

    public void setSugar_alc(BigDecimal sugalc) {
        this.sugar_alc = sugalc;
    }

    public BigDecimal getCalories() {
        calories = calories.setScale(mDecPlaces, BigDecimal.ROUND_HALF_EVEN);
        return calories;
    }

    public void setCalories(BigDecimal tcals) {
        this.calories = tcals;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public void setNumDecPlaces(Integer scale) {
        this.mDecPlaces = scale;
    }

}
