package edu.ucsd.calab.extrasensory.data;

import android.content.Context;
import android.graphics.Color;

import java.lang.reflect.Array;
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
    private static TreeMap<String,String[]> _secondaryActivitiesPerSubject = null;
    private static HashMap<String,Integer> _mainActivity2color = null;


    private static void initializeColorMap() {
        String[] mainActivities = getMainActivities();
        float maxHue = 250, minHue = 0;
        int numColors = mainActivities.length;

        _mainActivity2color = new HashMap<>(numColors);

        float decrement = (maxHue - minHue) / (numColors - 1);
        float[] hsv = new float[3];
        hsv[1] = 1;
        hsv[2] = 1;
        for (int i = 0; i < numColors; i ++) {
            hsv[0] = maxHue - i*decrement;
            _mainActivity2color.put(mainActivities[i], Color.HSVToColor(hsv));
        }
    }

    public static int getColorForMainActivity(String mainActivity) {
        if (_mainActivity2color == null) {
            initializeColorMap();
        }

        if (!_mainActivity2color.containsKey(mainActivity)) {
            return Color.WHITE;
        }

        return _mainActivity2color.get(mainActivity).intValue();
    }

    public static String[] getMainActivities() {
        if (_mainActivities == null) {
            _mainActivities = readLabelsFromFile(R.raw.main_activities_list,null);
        }

        return _mainActivities;
    }

    public static String[] getSecondaryActivities() {
        if (_secondaryActivities == null) {
            _secondaryActivitiesPerSubject = new TreeMap<>();
            _secondaryActivities = readLabelsFromFile(R.raw.secondary_activities_list,_secondaryActivitiesPerSubject);
        }

        return _secondaryActivities;
    }

    public static Map<String,String[]> getSecondaryActivitiesPerSubject() {
        // First, make sure the secondary activities (and subjects) are read for the first time:
        getSecondaryActivities();

        return _secondaryActivitiesPerSubject;
    }

    public static String[] getMoods() {
        if (_moods == null) {
            _moods = readLabelsFromFile(R.raw.moods_list,null);
        }

        return _moods;
    }

    public static String[] getHomeSensingLabels() {
        if (_homeSensingLabels == null) {
            _homeSensingLabels = readLabelsFromFile(R.raw.home_sensing_labels_list,null);
        }

        return _homeSensingLabels;
    }

    /**
     * Create a single String representation of the labels in the array, using Comma Separated Values.
     * This function assumes non of the components of the array are null
     * @param labels The labels to represent in a single String. Assumed that no string contains comma.
     * @return A single String representation of the array
     */
    public static String makeCSV(String[] labels) {
        if (labels == null || labels.length <= 0) {
            return "";
        }
        String csv = labels[0];
        for (int i = 1; i < labels.length; i ++) {
            csv = csv + "," + labels[i];
        }

        return csv;
    }

    /**
     * Create a single String representation of the labels (network-standardized version) in the array, using Comma Separate Values
     * This function assumes non of the components of the array are null
     * @param labels The labels to represent in a single String. Assumed that no string contains comma.
     * @return A single String representation of the array
     */
    public static String makeCSVForNetwork(String[] labels) {
        String[] standardLabels = standardizeLabelsForNetwork(labels);
        return makeCSV(standardLabels);
    }

    /**
     * Prepare labels string for transmission on the network.
     * The strings should then be more standard, without special characters that may appear in some labels.
     * @param labels
     * @return
     */
    public static String[] standardizeLabelsForNetwork(String[] labels) {
        String[] standardLabels = new String[labels.length];
        for (int i = 0; i < labels.length; i ++) {
            String label = labels[i];
            label = label.replaceAll(" ", "_");
            label = label.replaceAll("\\'", "_");
            label = label.replaceAll("\\(", "_");
            label = label.replaceAll("\\)", "_");
            label = label.toUpperCase();
            standardLabels[i] = label;
        }

        return standardLabels;
    }

    /**
     * Reverse the label names back to the original (nice human readable) format
     * from the standardized network format.
     * @param labelsFromNetwork
     * @return
     */
    public static  String[] reverseStandardizeLabelsFromNetwork(String[] labelsFromNetwork) {
        String[] originalFormatLabels = new String[labelsFromNetwork.length];
        for (int i = 0; i < labelsFromNetwork.length; i ++) {
            String label = labelsFromNetwork[i];
            if (label.endsWith("_")) {
                // Assume parentheses would be at the end of the label
                label = label.substring(0,label.length()-1) + ")";
            }
            label = label.replaceAll("__","_(");
            label = label.replaceAll("_"," ");
            // The standard version should be all upper case:
            label = label.substring(0,1) + label.substring(1).toLowerCase();
            label = label.replaceAll("i m","I\'m");
            label = label.replaceAll(" tv"," TV");
            originalFormatLabels[i] = label;
        }

        return originalFormatLabels;
    }

    /**
     * Read the labels from the text file in resources.raw.
     * The format of the file should be that every line has a single label,
     * and possibly with extra information (if after the labels there is a pipe '|' and then some more text).
     * The labels to extract here are just what's before the pipe (if it exists in the line).
     *
     * @param textFileResourceID ID of the raw resource text file
     * @param labelsPerSubject A map object into which to insert the relevant labels for each subject,
     *                         according to the content of the text file (if it has text after pipe).
     *                         If this given argument is null, disregard the text after pipe.
     * @return The array of labels read from the file
     */
    private static String[] readLabelsFromFile(int textFileResourceID,Map<String,String[]> labelsPerSubject) {
        Context context = ESApplication.getTheAppContext();
        ArrayList<String> parsedLabels = new ArrayList<String>();
        HashMap<String,ArrayList<String>> tempLabelsPerSubject = new HashMap<>(10);

        try {
            InputStream inputStream = context.getResources().openRawResource(textFileResourceID);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String[] splitLine;
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                splitLine = line.split("\\|");
                String label = splitLine[0];
                parsedLabels.add(label);
                if (labelsPerSubject != null && splitLine.length > 1) {
                    // Then lets read the subjects after the pipe:
                    String[] subjectsForLabel = splitLine[1].split(",");
                    for (String subject : subjectsForLabel) {
                        if (!tempLabelsPerSubject.containsKey(subject)) {
                            tempLabelsPerSubject.put(subject,new ArrayList<String>(10));
                        }
                        ((ArrayList<String>)tempLabelsPerSubject.get(subject)).add(label);
                    }
                }
            }
            bufferedReader.close();

         } catch (IOException e) {
                e.printStackTrace();
         }

        String[] labels = new String[parsedLabels.size()];
        for(int i=0; i < parsedLabels.size(); i++ ){
            labels[i] = parsedLabels.get(i);
        }

        if (labelsPerSubject != null) {
            // Then lets update the given map with the subject associations we read:
            for (String subject : tempLabelsPerSubject.keySet()) {
                ArrayList<String> subjectsLabels = (ArrayList<String>)tempLabelsPerSubject.get(subject);
                labelsPerSubject.put(subject,new String[subjectsLabels.size()]);
                subjectsLabels.toArray(labelsPerSubject.get(subject));
            }
        }

        return labels;
    }

}
