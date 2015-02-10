package edu.ucsd.calab.extrasensory.data;

import android.util.Log;

import java.util.Date;

/**
 * This class represents a timepoint with the relevant resolution for our application
 *
 * Created by Yonatan on 2/3/2015.
 */
public class ESTimestamp {

    private static final String LOG_TAG = "[ESTimestamp]";

    private int _secondsSinceEpoch;

    /**
     * Construct an ESTimestamp for a given Date object
     *
     * @param date The date of the timestamp
     */
    public ESTimestamp(Date date) {
        long millisSinceEpoch = date.getTime();
        long seconds = millisSinceEpoch / 1000l;
        this._secondsSinceEpoch = (int)seconds;
    }

    /**
     * Construct an ESTimestamp for right now
     */
    public ESTimestamp() {
        this(new Date());
    }

    @Override
    public String toString() {
        return "" + _secondsSinceEpoch;
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
     * Constructor with int value
     * @param secondsSinceEpoch
     */
    public ESTimestamp(int secondsSinceEpoch) {
        _secondsSinceEpoch = secondsSinceEpoch;
    }

    /**
     * Get the number of seconds since the epoch.
     * @return
     */
    int get_secondsSinceEpoch() {
        return _secondsSinceEpoch;
    }

    /**
     * Get the time difference (in seconds) from the given argument to this ESTimestamp
     *
     * @param minusTime The timestamp to subtract from this ESTimestamp
     * @return
     */
    int differenceInSeconds(ESTimestamp minusTime) {
        if (minusTime == null) {
            String msg = "null ESTimestamp given as argument.";
            Log.e(LOG_TAG,msg);
            throw new NullPointerException(msg);
        }

        return this._secondsSinceEpoch - minusTime._secondsSinceEpoch;
    }
}
