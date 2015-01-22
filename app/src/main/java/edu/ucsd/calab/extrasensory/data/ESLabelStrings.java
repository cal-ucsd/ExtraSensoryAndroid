package edu.ucsd.calab.extrasensory.data;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;

/**
 * This class supplies the vocabulary of labels for the different categories (main, secondary, mood),
 * as well as index-subjects for labels and color associations.
 *
 * Created by Yonatan on 1/21/2015.
 */
public class ESLabelStrings {

    private static String[] _mainActivities = null;
    private static String[] _secondaryActivities = null;
    private static String[] _moods = null;
    private static String[] _homeSensingLabels = null;


    public static String[] getMainActivities() {
        if (_mainActivities == null) {
  //          _mainActivities = readLabelsFromFile(R.raw.mainActivitiesList);
        }

        return _mainActivities;
    }

    public static String[] getSecondaryActivities() {
        if (_secondaryActivities == null) {
//            _secondaryActivities = readLabelsFromFile(R.raw.secondary_activities_list);
        }

        return _secondaryActivities;
    }

    public static String[] getMoods() {
        if (_moods == null) {
//            _moods = readLabelsFromFile(R.raw.moodsList);
        }

        return _moods;
    }

    public static String[] getHomeSensingLabels() {
        if (_homeSensingLabels == null) {
    //        _homeSensingLabels = readLabelsFromFile(R.raw.homeSensingLabelsList);
        }

        return _homeSensingLabels;
    }

    /**
     * Read the labels from the text file in resources.raw.
     * The format of the file should be that every line has a single label,
     * and possibly with extra information (if after the labels there is a pipe '|' and then some more text).
     * The labels to extract here are just what's before the pipe (if it exists in the line).
     *
     * @param textFileResourceID ID of the raw resource text file
     * @return The array of labels read from the file
     */
    private static String[] readLabelsFromFile(int textFileResourceID) {
        Context context = ESApplication.getTheAppContext();
        InputStream inputStream = context.getResources().openRawResource(textFileResourceID);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        //TODO read line by line, parse each line (if has '|' character then the label is what's before the '|')
        return null;
    }

}
