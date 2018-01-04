package edu.ucsd.calab.extrasensory.data;

import java.util.Arrays;

/**
 * A simple struct to contain a label set.
 * This class is not associated/synchronized with the DB, not affiliated with any specific activity record (no timestamp),
 * and you can freely set it's instances' properties.
 * Use instances of this class to temporarily maintain a set of labels.
 *
 * Created by Yonatan on 4/15/2015.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class ESLabelStruct {

    public String _mainActivity = null;
    public String[] _secondaryActivities = new String[0];
    public String[] _moods = new String[0];

    public ESLabelStruct() {}

    public ESLabelStruct(ESLabelStruct other) {
        if (other == null) {
            // Leave default values
            return;
        }

        this._mainActivity = other._mainActivity;
        if (other._secondaryActivities != null) {
            this._secondaryActivities = Arrays.copyOf(other._secondaryActivities, other._secondaryActivities.length);
        }
        if (other._moods != null) {
            this._moods = Arrays.copyOf(other._moods, other._moods.length);
        }
    }

}
