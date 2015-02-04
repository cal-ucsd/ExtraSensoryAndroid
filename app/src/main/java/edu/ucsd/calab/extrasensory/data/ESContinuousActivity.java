package edu.ucsd.calab.extrasensory.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ESContinuousActivity represents an activity that has a duration longer than a minute,
 * and corresponds to an array of consecutive ESActivity records/objects
 * that all have exactly the same labels (main, secondary and mood).
 * In case the activities have user-correction main activity label, the server prediction will be disregarded.
 *
 * Created by Yonatan on 1/20/2015.
 */
public class ESContinuousActivity {

    private static final int MAX_TIME_GAP_FOR_MERGING_ACTIVITIES = 120;

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
    public ESTimestamp getStartTimestamp() {
        if (this.isEmpty()) {
            return null;
        }

        return _minuteActivities[0].get_timestamp();
    }

    /**
     * Get the timestamp of the last atomic (minute) activity in this continuous activity.
     *
     * @return The last timestamp of this continuous activity
     */
    public ESTimestamp getEndTimestamp() {
        if (this.isEmpty()) {
            return null;
        }
        if (_minuteActivities[this.getDurationInMinutes()-1] == null) {
            return null;
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

        for (ESActivity minuteActivity : _minuteActivities) {
            if (minuteActivity != null) {
                String pred = minuteActivity.get_mainActivityServerPrediction();
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
     * Consecutive ESActivity objects in the array with timestamps of 120 seconds apart or more
     * will be considered as belonging to two separate continuous activities.
     *
     * @param minuteActivities The sequence of atomic activities (assumed sorted in ascending order of timestamp)
     * @return The sequence of continuous activities, sorted in ascending order of start-timestamp
     */
    public static ESContinuousActivity[] mergeContinuousActivities(ESActivity[] minuteActivities) {
        ArrayList<ESContinuousActivity> continuousActivities = new ArrayList<ESContinuousActivity>(minuteActivities.length);
        ArrayList<ESActivity> mergedActivities = new ArrayList<ESActivity>(minuteActivities.length);

        for (ESActivity minuteActivity : minuteActivities) {
            if (mergedActivities.isEmpty()) {
                // Then we are free to start adding similar activities:
                mergedActivities.add(minuteActivity);
                continue;
            }

            // Collected activities are not empty, so compare new activity to the last one:
            if (shouldMergeTwoAtomicActivities(mergedActivities.get(mergedActivities.size() - 1), minuteActivity)) {
                mergedActivities.add(minuteActivity);
            } else {
                // Then we should close the sequence of minute activities so far, and start a new one:
                ESActivity[] activities = mergedActivities.toArray(new ESActivity[mergedActivities.size()]);
                ESContinuousActivity continuousActivity = new ESContinuousActivity(activities);
                continuousActivities.add(continuousActivity);

                // Start the new sequence of minute activities:
                mergedActivities.clear();
                mergedActivities.add(minuteActivity);
            }
        }

        // Handle the last sequence of minute activities:
        if (!mergedActivities.isEmpty()) {
            ESActivity[] activities = mergedActivities.toArray(new ESActivity[mergedActivities.size()]);
            ESContinuousActivity continuousActivity = new ESContinuousActivity(activities);
            continuousActivities.add(continuousActivity);
        }

        return continuousActivities.toArray(new ESContinuousActivity[continuousActivities.size()]);
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
        Set<String> set1 = getSetFromArray(labelSet1);
        Set<String> set2 = getSetFromArray(labelSet2);

        return set1.equals(set2);
    }

    private static Set<String> getSetFromArray(String[] array) {
        HashSet<String> set = new HashSet<String>();
        Collections.addAll(set, array);

        return set;
    }

    private static boolean shouldMergeTwoAtomicActivities(ESActivity firstActivity,ESActivity secondActivity) {
        // Compare timestamps:
        int timeGap = secondActivity.get_timestamp().differenceInSeconds(firstActivity.get_timestamp());
        if (timeGap > MAX_TIME_GAP_FOR_MERGING_ACTIVITIES) {
            return false;
        }

        // Compare main activity:
        if (firstActivity.hasUserCorrectedMainLabel() && !secondActivity.hasUserCorrectedMainLabel()) {
            return false;
        }
        if (!firstActivity.hasUserCorrectedMainLabel() && secondActivity.hasUserCorrectedMainLabel()) {
            return false;
        }
        String main1 = firstActivity.mostUpToDateMainActivity();
        String main2 = secondActivity.mostUpToDateMainActivity();
        if (main1 != null) {
            if (!main1.equals(main2)) {
                return false;
            }
        }
        else {
            if (main2 != null) {
                return false;
            }
        }
        // If reached here, main activity compares fine.

        // Compare secondary activities:
        if (!areTwoSetsOfLabelsTheSame(firstActivity.get_secondaryActivities(),secondActivity.get_secondaryActivities())) {
            return false;
        }

        // Compare moods:
        if (!areTwoSetsOfLabelsTheSame(firstActivity.get_moods(),secondActivity.get_moods())) {
            return false;
        }

        // If reached here, everything compares fine.
        return true;
    }

}
