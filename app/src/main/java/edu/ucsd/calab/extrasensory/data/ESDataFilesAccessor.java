package edu.ucsd.calab.extrasensory.data;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;

/**
 * This class handles the main interface of ExtraSensory App with external apps on the phone that wish to use data from ExtraSensory App.
 * This is done through writing to text files (in JSON format) to the internal storage of the phone,
 * in a way that any other app can read these files.
 *
 * This can be useful, if you want to develop an app that uses context-recognition (provided by ExtraSensory App, running in the background)
 * without manipulating the code of ExtraSensory App.
 *
 * Created by Yonatan on 6/21/2017.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class ESDataFilesAccessor {

    private static final String LOG_TAG = "[ESDataFilesAccessor]";
    private static final String LABEL_DATA_DIRNAME = "extrasensory.labels." + ESSettings.uuid().substring(0,8);
    private static final Context CONTEXT = ESApplication.getTheAppContext();
    private static final String LABEL_NAMES_KEY = "label_names";
    private static final String LABEL_PROBS_KEY = "label_probs";
    private static final String LOCATION_LAT_LONG_KEY = "location_lat_long";

    public static final String BROADCAST_SAVED_PRED_FILE = "edu.ucsd.calab.extrasensory.broadcast.saved_prediction_file";
    public static final String BROADCAST_EXTRA_KEY_TIMESTAMP = "timestamp";


    private static File getLabelFilesDir() throws IOException {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.e(LOG_TAG,"!!! External storage is not mounted.");
            throw new IOException("External storage is not mounted.");
        }

        File dataFilesDir = new File(CONTEXT.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),LABEL_DATA_DIRNAME);
//        File dataFilesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),LABEL_DATA_DIRNAME);
        if (!dataFilesDir.exists()) {
            // Create the directory:
            if (!dataFilesDir.mkdirs()) {
                Log.e(LOG_TAG,"!!! Failed creating directory: " + dataFilesDir.getPath());
                throw new IOException("Failed creating directory: " + dataFilesDir.getPath());
            }
        }

        return dataFilesDir;
    }

    /**
     * Write the server predictions for an instance to a textual file that will be available to other apps.
     * @param timestamp The timestamp of the instance
     * @param predictedLabelNames The array of labels in the prediction
     * @param predictedLabelProbs The corresponding probabilities assigned to the labels
     * @param locationLatLong The location coordinates [latitude, longitude] of the representative location for this minute, analyzed by the server.
     * @return Did we succeed writing the file
     */
    public static boolean writeServerPredictions(ESTimestamp timestamp,String[] predictedLabelNames,double[] predictedLabelProbs,double[] locationLatLong) {
        final File instanceLabelsFile;
        try {
            instanceLabelsFile = new File(getLabelFilesDir(),timestamp.toString() + ".server_predictions.json");
        } catch (IOException e) {
            return false;
        }

        // Construct the JSON structure:
        JSONObject jsonObject = new JSONObject();
        JSONArray labelNamesArray = new JSONArray();
        JSONArray labelProbsArray = new JSONArray();
        JSONArray latLongArray = null;
        try {
            if (predictedLabelNames != null && predictedLabelProbs != null) {
                if (predictedLabelNames.length != predictedLabelProbs.length) {
                    Log.e(LOG_TAG, "Got inconsistent length of label names and label probs. Not writing file.");
                    return false;
                }

                for (int i = 0; i < predictedLabelNames.length; i++) {
                    labelNamesArray.put(i, predictedLabelNames[i]);
                    labelProbsArray.put(i,predictedLabelProbs[i]);
                }
            }

            jsonObject.put(LABEL_NAMES_KEY,labelNamesArray);
            jsonObject.put(LABEL_PROBS_KEY,labelProbsArray);

            // Add the location coordinates:
            if (locationLatLong != null && locationLatLong.length == 2) {
                latLongArray = new JSONArray();
                latLongArray.put(locationLatLong[0]);
                latLongArray.put(locationLatLong[1]);
            }
            jsonObject.put(LOCATION_LAT_LONG_KEY,latLongArray);
        }
        catch (JSONException e) {
            Log.e(LOG_TAG,"Failed forming a json object for server predictions");
            return false;
        }

        try {
            FileOutputStream fos = new FileOutputStream(instanceLabelsFile);
            fos.write(jsonObject.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG,"!!! File not found: " + instanceLabelsFile.getPath());
            return false;
        } catch (IOException e) {
            Log.e(LOG_TAG,"!!! Failed to write to json file: " + instanceLabelsFile.getPath());
            return false;
        }

        // If we reached here, everything was fine and we were able to write the JSON file
        Log.d(LOG_TAG,">> Saved labels file: " + instanceLabelsFile.getPath());
        // Add it to the media scanner:
        MediaScannerConnection.scanFile(CONTEXT,
                new String[]{instanceLabelsFile.getAbsolutePath()}, new String[]{"application/json"},
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.d(LOG_TAG,"++ Completed scan for file " + instanceLabelsFile.getPath());
                    }
                });

        // Announce to whoever is listening that there is a new saved file:
        Intent intent = new Intent(BROADCAST_SAVED_PRED_FILE);
        intent.putExtra(BROADCAST_EXTRA_KEY_TIMESTAMP,timestamp.toString());
        ESApplication.getTheAppContext().sendBroadcast(intent);
        Log.d(LOG_TAG,"Sent broadcast message about saving prediction file for timestamp " + timestamp.toString());

        return true;
    }

    /**
     * Write the user-reported labels for an instance to a textual file that will be available to other apps.
     * @param timestamp The timestamp of the minute-instance
     * @param mainActivityUserCorrection - the main-activity labels, corrected by the user
     * @param secondaryActivities - the secondary activities reported by the user
     * @param moods - the mood labels provided by the user
     * @return Did we succeed writing the file
     */
    public static boolean writeUserReportedLabels(ESTimestamp timestamp,String mainActivityUserCorrection,String[] secondaryActivities,String[] moods) {
        final File instanceLabelsFile;
        try {
            instanceLabelsFile = new File(getLabelFilesDir(),timestamp.toString() + ".user_reported_labels.json");
        } catch (IOException e) {
            return false;
        }

        // Prepare a set of reported labels (all combined together):
        Set<String> userLabelsSet = new HashSet<>(10);
        String dummyLabel = ESApplication.getTheAppContext().getString(R.string.not_sure_dummy_label);
        if (mainActivityUserCorrection != null && !mainActivityUserCorrection.equals(dummyLabel)) {
            userLabelsSet.add(mainActivityUserCorrection);
        }
        if (secondaryActivities != null) {
            for (String secLabel : secondaryActivities) {
                userLabelsSet.add(secLabel);
            }
        }
        if (moods != null) {
            for (String moodLabel : moods) {
                userLabelsSet.add(moodLabel);
            }
        }

        // Construct the JSON structure:
        JSONArray labelNamesJsonArray = new JSONArray();
        String[] userLabels = new String[userLabelsSet.size()];
        userLabelsSet.toArray(userLabels);
        for (String userLabel : userLabels) {
            labelNamesJsonArray.put(userLabel);
        }

        try {
            FileOutputStream fos = new FileOutputStream(instanceLabelsFile);
            fos.write(labelNamesJsonArray.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG,"!!! File not found: " + instanceLabelsFile.getPath());
            return false;
        } catch (IOException e) {
            Log.e(LOG_TAG,"!!! Failed to write to json file: " + instanceLabelsFile.getPath());
            return false;
        }

        // If we reached here, everything was fine and we were able to write the JSON file
        Log.d(LOG_TAG,">> Saved labels file: " + instanceLabelsFile.getPath());
        // Add it to the media scanner:
        MediaScannerConnection.scanFile(CONTEXT,
                new String[]{instanceLabelsFile.getAbsolutePath()}, new String[]{"application/json"},
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.d(LOG_TAG,"++ Completed scan for file " + instanceLabelsFile.getPath());
                    }
                });

        return true;
    }
}
