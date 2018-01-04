package edu.ucsd.calab.extrasensory.data;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class represents a timepoint with the relevant time-resolution for our application
 *
 * Created by Yonatan on 2/3/2015.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class ESTimestamp {

    private static final String LOG_TAG = "[ESTimestamp]";

    private static final int SECONDS_IN_24_HOURS = 86400;
    private static final long MILLISECONDS_IN_SECOND = 1000l;

    private int _secondsSinceEpoch;

    /**
     * Construct an ESTimestamp for a given Date object
     *
     * @param date The date of the timestamp
     */
    public ESTimestamp(Date date) {
        long millisSinceEpoch = date.getTime();
        long seconds = millisSinceEpoch / MILLISECONDS_IN_SECOND;
        this._secondsSinceEpoch = (int)seconds;
    }

    /**
     * Construct an ESTimestamp for right now
     */
    public ESTimestamp() {
        this(new Date());
    }

    /**
     * Constructor with int value
     * @param secondsSinceEpoch
     */
    public ESTimestamp(int secondsSinceEpoch) {
        _secondsSinceEpoch = secondsSinceEpoch;
    }

    /**
     * Use this constructor to get the timestamp based on the reference timestamp,
     * plus an integer number of days.
     * @param reference The reference timestamp to calculate from
     * @param daysOffset Complete number of days to add.
     *                   E.g. -1 if you want 24 hours before the reference,
     *                   +2 if you want 48 hours after the reference.
     */
    public ESTimestamp(ESTimestamp reference,int daysOffset) {
        this(reference.get_secondsSinceEpoch() + daysOffset*SECONDS_IN_24_HOURS);
    }

    /**
     * Initialize a timestamp using a string representation (of the seconds since epoch)
     * @param secondsSinceEpochStr
     */
    public ESTimestamp(String secondsSinceEpochStr) {
        this(Integer.parseInt(secondsSinceEpochStr));
    }

    public static ESTimestamp getStartOfTodayTimestamp() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY,00);
        cal.set(Calendar.MINUTE,00);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        Date date = cal.getTime();
        return new ESTimestamp(date);
    }

    @Override
    public String toString() {
        return "" + _secondsSinceEpoch;
    }

    public Calendar toCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(MILLISECONDS_IN_SECOND * _secondsSinceEpoch);
        return calendar;
    }

    public int getHourOfDayOutOf24() {
        return toCalendar().get(Calendar.HOUR_OF_DAY);
    }

    public int getMinuteOfHour() {
        return toCalendar().get(Calendar.MINUTE);
    }

    public String infoString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EE MMM dd H:m", Locale.US);
        return "" + _secondsSinceEpoch + " (" + dateFormat.format(this.getDateOfTimestamp()) + ")";
    }

    public boolean equals(ESTimestamp other) {
        if (other == null) {
            return false;
        }

        return this._secondsSinceEpoch == other._secondsSinceEpoch;
    }

    public boolean isEarlierThan(ESTimestamp other) {
        if (other == null) {
            String msg = "null ESTimestamp given";
            Log.e(LOG_TAG,msg);
            throw new NullPointerException(msg);
        }

        return this._secondsSinceEpoch < other._secondsSinceEpoch;
    }

    public boolean isLaterThan(ESTimestamp other) {
        if (other == null) {
            String msg = "null ESTimestamp given";
            Log.e(LOG_TAG,msg);
            throw new NullPointerException(msg);
        }

        return this._secondsSinceEpoch > other._secondsSinceEpoch;
    }

    /**
     * Get the date/time (in accuracy of seconds) that this timestamp represents.
     * @return The data/time represented by this timestamp
     */
    public Date getDateOfTimestamp() {
        return new Date(this._secondsSinceEpoch * MILLISECONDS_IN_SECOND);
    }

    /**
     * Get the number of seconds since the epoch.
     * @return
     */
    public int get_secondsSinceEpoch() {
        return _secondsSinceEpoch;
    }

    /**
     * Get the time difference (in seconds) from the given argument to this ESTimestamp
     *
     * @param minusTime The timestamp to subtract from this ESTimestamp
     * @return
     */
    public int differenceInSeconds(ESTimestamp minusTime) {
        if (minusTime == null) {
            String msg = "null ESTimestamp given as argument.";
            Log.e(LOG_TAG,msg);
            throw new NullPointerException(msg);
        }

        return this._secondsSinceEpoch - minusTime._secondsSinceEpoch;
    }


}
