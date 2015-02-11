package edu.ucsd.calab.extrasensory.data;

import android.content.Context;

import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
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
            _mainActivities = readLabelsFromFile(R.raw.main_activities_list);
        }

        return _mainActivities;
    }

    public static String[] getSecondaryActivities() {
        if (_secondaryActivities == null) {
            _secondaryActivities = readLabelsFromFile(R.raw.secondary_activities_list);
        }

        return _secondaryActivities;
    }

    public static String[] getMoods() {
        if (_moods == null) {
            _moods = readLabelsFromFile(R.raw.moods_list);
        }

        return _moods;
    }

    public static String[] getHomeSensingLabels() {
        if (_homeSensingLabels == null) {
            _homeSensingLabels = readLabelsFromFile(R.raw.home_sensing_labels_list);
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
        ArrayList<String> parsedLabels = new ArrayList<String>();

        try {
            InputStream inputStream = context.getResources().openRawResource(textFileResourceID);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String[] splitLabel;
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                splitLabel = line.split("\\|");
                parsedLabels.add(splitLabel[0]);
            }
            bufferedReader.close();

         } catch (IOException e) {
                e.printStackTrace();
         }

        String[] labels = new String[parsedLabels.size()];
        for(int i=0; i < parsedLabels.size(); i++ ){
            labels[i] = parsedLabels.get(i);
        }
        return labels;
    }

}
