package edu.ucsd.calab.extrasensory;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.util.Date;

import edu.ucsd.calab.extrasensory.data.ESActivity;
import edu.ucsd.calab.extrasensory.data.ESLabelStrings;
import edu.ucsd.calab.extrasensory.data.ESLabelStruct;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;

/**
 * This class serves as a central location to manage various aspects of the application,
 * and as a shared place to be reached from all the components of the application.
 *
 * Created by Yonatan on 1/15/2015.
 */
public class ESApplication extends Application {

    private static final String LOG_TAG = "[ESApplication]";
    private static final long WAIT_BEFORE_START_FIRST_RECORDING_MILLIS = 500;
    private static final long RECORDING_SESSIONS_INTERVAL_MILLIS = 1000*60;
    private static final String ZIP_DIR_NAME = "zip";
    private static final String DATA_DIR_NAME = "data";
    private static final String FEEDBACK_DIR_NAME = "feedback";

    private static Context _appContext;

    public static Context getTheAppContext() {
        return _appContext;
    }

    public static class PredeterminedLabels {
        private ESLabelStruct _labels = null;
        private boolean _startedFirstActivityRecording = false;
        private ESTimestamp _validUntil = new ESTimestamp(0);

        private void clearLabels() {
            _labels = null;
            _startedFirstActivityRecording = false;
            _validUntil = new ESTimestamp(0);
        }

        public ESLabelStruct getLabels() {
            if (_labels == null || _validUntil == null || _validUntil.isEarlierThan(new ESTimestamp())) {
                // Then there are no predetermined labels, or those that are here are no longer valid:
                clearLabels();
            }
            return _labels;
        }

        boolean is_startedFirstActivityRecording() {
            return _startedFirstActivityRecording;
        }

        void set_startedFirstActivityRecording(boolean startedFirstActivityRecording) {
            _startedFirstActivityRecording = startedFirstActivityRecording;
        }

        private void setPredeterminedLabels(ESLabelStruct labels,int validForHowManyMinutes) {
            _labels = new ESLabelStruct(labels);
            _startedFirstActivityRecording = false;
            //TODO: set valid until
//            _validUntil = new ESTimestamp(new ESTimestamp().)
        }
    }

    static PredeterminedLabels _predeterminedLabels = new PredeterminedLabels();

    public static File getZipDir() {
        return getTheAppContext().getDir(ZIP_DIR_NAME, Context.MODE_PRIVATE);
    }

    public static File getDataDir() {
        return getTheAppContext().getDir(DATA_DIR_NAME, Context.MODE_PRIVATE);
    }

    public static File getFeedbackDir() {
        return getTheAppContext().getDir(FEEDBACK_DIR_NAME, Context.MODE_PRIVATE);
    }

    private ESSensorManager _sensorManager;
    private AlarmManager _alarmManager;
    private boolean _userSelectedDataCollectionOn = true;

    /**
     * Is data collection "on" now, according to the user decision (not according to storage limiation)
     * @return
     */
    public boolean is_userSelectedDataCollectionOn() {
        return _userSelectedDataCollectionOn;
    }

    /**
     * Mark the user input for allowing/stopping data collection now.
     * @param userSelectedDataCollectionOn
     */
    public void set_userSelectedDataCollectionOn(boolean userSelectedDataCollectionOn) {
        if (_userSelectedDataCollectionOn == userSelectedDataCollectionOn) {
            // Then there is nothing to do:
            return;
        }

        _userSelectedDataCollectionOn = userSelectedDataCollectionOn;
        if (_userSelectedDataCollectionOn) {
            startRecordingSchedule();
        }
        else {
            stopCurrentRecordingAndRecordingSchedule();
        }
    }

    public static boolean debugMode() { return false; }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(LOG_TAG, "Application being created.");
        _appContext = getApplicationContext();

        _sensorManager = ESSensorManager.getESSensorManager();
        _alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);


        // Start the scheduling of periodic recordings:
        startRecordingSchedule();
    }

    /**
     * Start user initiated recording (and schedule).
     * This function first stops any current recording (if there is one), and stops the recording schedule,
     * then starts a new recording schedule.
     * The given labels are used for the started recording from now until the end time specified by the user.
     *
     * @param labelsToAssign - The labels to assign to the following activities
     * @param validForHowMinutes - How many minutes should the given labels be automatically assigned?
     */
    public void startActiveFeedback(ESLabelStruct labelsToAssign,int validForHowMinutes) {
        _predeterminedLabels.setPredeterminedLabels(labelsToAssign,validForHowMinutes);
        stopCurrentRecordingAndRecordingSchedule();
        startRecordingSchedule();
    }

    /**
     * Start a repeating schedule of recording sessions (every 1 minute) from now.
     */
    private void startRecordingSchedule() {
        if (_alarmManager == null) {
            Log.e(LOG_TAG,"Alarm manager is null");
            return;
        }

        PendingIntent pendingIntent = createESPendingIntent(ESIntentService.ACTION_START_RECORDING);
        // Before registering the repeating alarm, make sure that any similar alarm is canceled:
        _alarmManager.cancel(pendingIntent);

        Log.i(LOG_TAG,"Scheduling the recording sessions with interval of " + RECORDING_SESSIONS_INTERVAL_MILLIS/1000 + " seconds.");
        _alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                WAIT_BEFORE_START_FIRST_RECORDING_MILLIS,
                RECORDING_SESSIONS_INTERVAL_MILLIS,
                pendingIntent);
    }

    private PendingIntent createESPendingIntent(String action) {
        Intent intent = new Intent(getApplicationContext(),ESIntentService.class);
        intent.setAction(action);

        return PendingIntent.getService(getApplicationContext(),0,intent,0);
    }

    /**
     * Stop the current recording session (if one exists),
     * and cancel the periodic schedule of recording sessions.
     */
    private void stopCurrentRecordingAndRecordingSchedule() {
        stopRecordingSchedule();
        stopCurrentRecording();
    }

    private void stopRecordingSchedule() {
        if (_alarmManager == null) {
            Log.e(LOG_TAG,"Alarm manager is null");
            return;
        }

        PendingIntent pendingIntentReference = createESPendingIntent(ESIntentService.ACTION_START_RECORDING);
        _alarmManager.cancel(pendingIntentReference);
        Log.i(LOG_TAG,"Stopped the repeated recording schedule.");
    }

    private void stopCurrentRecording() {
        Log.i(LOG_TAG,"Stopping current recording session.");
        _sensorManager.stopRecordingSensors();
    }
}
