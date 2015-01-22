package edu.ucsd.calab.extrasensory.data;

/**
 * ESContinuousActivity represents an activity that has a duration longer than a minute,
 * and corresponds to an array of consecutive ESActivity records/objects
 * that all have exactly the same labels (main, secondary and mood).
 * In case the activities have user-correction main activity label, the server prediction will be disregarded.
 *
 * Created by Yonatan on 1/20/2015.
 */
public class ESContinuousActivity {

    public static final int EMPTY_ACTIVITY_TIMESTAMP = -1;

    private ESActivity[] _minuteActivities;
    private ESContinuousActivity(ESActivity[] minuteActivities) {
        _minuteActivities = minuteActivities;
    }

    /**
     * Is the continuous activity empty (representing no activity)?
     * @return TRUE iff the activity is empty
     */
    public boolean isEmpty() {
        return ((_minuteActivities == null) || (_minuteActivities.length <= 0) || (_minuteActivities[0] == null));
    }

    /**
     * Get the approximate duration of the activity.
     * Actually get the number of atomic (minute) consecutive activities represented by this continuous activity.
     * This should approximately be the number of minutes this activity took.
     *
     * @return The approximate number of minutes this continuous activity took.
     */
    public int getDurationInMinutes() {
        if (this.isEmpty()) {
            return 0;
        }

        return _minuteActivities.length;
    }

    /**
     * Get the timestamp of the start of this continuous activity.
     * @return The start timestamp
     */
    public int getStartTimestamp() {
        if (this.isEmpty()) {
            return EMPTY_ACTIVITY_TIMESTAMP;
        }

        return _minuteActivities[0].get_timestamp();
    }

    /**
     * Get the timestamp of the last atomic (minute) activity in this continuous activity.
     *
     * @return The last timestamp of this continuous activity
     */
    public int getEndTimestamp() {
        if (this.isEmpty()) {
            return EMPTY_ACTIVITY_TIMESTAMP;
        }
        if (_minuteActivities[this.getDurationInMinutes()-1] == null) {
            return EMPTY_ACTIVITY_TIMESTAMP;
        }

        return _minuteActivities[this.getDurationInMinutes()-1].get_timestamp();
    }

    /**
     * Get a server prediction of the main activity label.
     * Search for the first atomic activity that has a non-null server prediction.
     *
     * @return A server prediction of main activity, or null if none was found.
     */
    public String getMainActivityServerPrediction() {
        if (this.isEmpty()) {
            return null;
        }

        for (int i=0; i < _minuteActivities.length; i++) {
            if (_minuteActivities[i] != null) {
                String pred = _minuteActivities[i].get_mainActivityServerPrediction();
                if (pred != null) {
                    return pred;
                }
            }
        }

        return null;
    }

    /**
     * Get the user correction of the main activity label.
     *
     * @return The user correction of main activity, or null if none was found.
     */
    public String getMainActivityUserCorrection() {
        if (this.isEmpty()) {
            return null;
        }

        for (ESActivity minuteActivity : _minuteActivities) {
            if (minuteActivity != null) {
                String correction = minuteActivity.get_mainActivityUserCorrection();
                if (correction != null) {
                    return correction;
                }
            }
        }

        return null;
    }

    /**
     * Get the array of secondary activity labels associated with this continuous activity.
     * The order of the secondary activities is arbitrary.
     *
     * @return The array of secondary activities
     */
    public String[] getSecondaryActivities() {
        if (this.isEmpty()) {
            return null;
        }

        return _minuteActivities[0].get_secondaryActivities();
    }

    /**
     * Get the array of mood labels associated with this continuous activity.
     * The order of the moods is arbitrary.
     *
     * @return The array of moods.
     */
    public  String[] getMoods() {
        if (this.isEmpty()) {
            return null;
        }

        return _minuteActivities[0].get_moods();
    }


    // Static interface:


    /**
     * Create an array of continuous activities out of an array of atomic activities,
     * by merging every sequence of consecutive atomic activities with the same labels into a single
     * continuous activity object. For collections of labels (secondary activities or moods) the order
     * of the Strings in the array doesn't matter, just the set of strings.
     *
     * @param minuteActivities The sequence of atomic activities (assumed sorted in ascending order of timestamp)
     * @return The sequence of continuous activities, sorted in ascending order of start-timestamp
     */
    public static ESContinuousActivity[] mergeContinuousActivities(ESActivity[] minuteActivities) {
        //TODO.................
        return null;
    }

    /**
     * Compare the two sets of labels, ignoring the orders,
     * to see if both sets contain exactly the same labels.
     *
     * @param labelSet1 Array of labels of set1 (arbitrary order)
     * @param labelSet2 Array of labels of set2 (arbitrary order)
     * @return True iff both sets contain exactly the same labels
     */
    private static boolean areTwoSetsOfLabelsTheSame(String[] labelSet1,String[] labelSet2) {
        //TODO..............
        return false;
    }
}
