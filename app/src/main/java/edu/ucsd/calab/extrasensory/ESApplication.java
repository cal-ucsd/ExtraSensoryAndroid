package edu.ucsd.calab.extrasensory;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.util.Date;

import edu.ucsd.calab.extrasensory.data.ESActivity;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESLabelStrings;
import edu.ucsd.calab.extrasensory.data.ESLabelStruct;
import edu.ucsd.calab.extrasensory.data.ESSettings;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;

/**
 * This class serves as a central location to manage various aspects of the application,
 * and as a shared place to be reached from all the components of the application.
 *
 * Created by Yonatan on 1/15/2015.
 */
public class ESApplication extends Application {

    private static final String LOG_TAG = "[ESApplication]";
    private static final long WAIT_BEFORE_START_FIRST_RECORDING_MILLIS = 4000;
    private static final long RECORDING_SESSIONS_INTERVAL_MILLIS = 1000*60;
    private static final long MILLISECONDS_IN_MINUTE = 1000*60;
    private static final long RECENT_TIME_PERIOD_IN_MILLIS = 20*MILLISECONDS_IN_MINUTE;
    private static final String ZIP_DIR_NAME = "zip";
    private static final String DATA_DIR_NAME = "data";
    private static final String FEEDBACK_DIR_NAME = "feedback";

    private static final String NOTIFICATION_TITLE = "ExtraSensory";
    private static final String NOTIFICATION_TEXT_NO_VERIFIED = "Can you please report what you are doing?";
    private static final String NOTIFICATION_BUTTON_TEXT_YES = "Yes";
    private static final String NOTIFICATION_BUTTON_TEXT_NOT_NOW = "Not now";

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

            long gracePeriod = 2000;
            Date validUntilDate = new Date(new Date().getTime() + validForHowManyMinutes*MILLISECONDS_IN_MINUTE + gracePeriod);
            _validUntil = new ESTimestamp(validUntilDate);
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

    private BroadcastReceiver _broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ESNetworkAccessor.BROADCAST_NETWORK_QUEUE_SIZE_CHANGED.equals(intent.getAction())) {
                // Check if we're on the limit of storage capacity: we may need to turn on/off data collection:
                int qsize = ESNetworkAccessor.getESNetworkAccessor().uploadQueueSize();
                if (qsize >= ESSettings.maxStoredExamples()-1) {
                    checkShouldWeCollectDataAndManageAppropriately();
                }
            }
        }
    };

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
        checkShouldWeCollectDataAndManageAppropriately();
    }

    private void turnDataCollectionOff() {
        stopCurrentRecordingAndRecordingSchedule();
        stopNotificationSchedule();
    }

    private void turnDataCollectionOn() {
        startRecordingSchedule(0);
        startNotificationSchedule();
    }

    public boolean shouldDataCollectionBeOn() {
        if (!_userSelectedDataCollectionOn) {
            // Then user doesn't allow data collection right now:
            return false;
        }
        if (ESNetworkAccessor.getESNetworkAccessor().uploadQueueSize() >= ESSettings.maxStoredExamples()) {
            // Then we're already storing max capacity and we shouldn't collect more data:
            return false;
        }
        return true;
    }

    public void checkShouldWeCollectDataAndManageAppropriately() {
        if (shouldDataCollectionBeOn()) {
            turnDataCollectionOn();
        }
        else {
            turnDataCollectionOff();
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

        LocalBroadcastManager.getInstance(_appContext).registerReceiver(_broadcastReceiver,new IntentFilter(ESNetworkAccessor.BROADCAST_NETWORK_QUEUE_SIZE_CHANGED));

        // Start the scheduling of periodic recordings:
        startRecordingSchedule(WAIT_BEFORE_START_FIRST_RECORDING_MILLIS);
        // Start the notification schedule:
        startNotificationSchedule();
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
        _predeterminedLabels.setPredeterminedLabels(labelsToAssign, validForHowMinutes);
        stopCurrentRecordingAndRecordingSchedule();
        startRecordingSchedule(0);
    }

    /**
     * Start a repeating schedule of recording sessions (every 1 minute).
     *
     * @param millisWaitBeforeStart - time to wait (milliseconds) before the first recording session in this schedule.
     */
    private void startRecordingSchedule(long millisWaitBeforeStart) {
        if (_alarmManager == null) {
            Log.e(LOG_TAG,"Alarm manager is null");
            return;
        }

        PendingIntent pendingIntent = createESPendingIntent(ESIntentService.ACTION_START_RECORDING);
        // Before registering the repeating alarm, make sure that any similar alarm is canceled:
        _alarmManager.cancel(pendingIntent);

        Log.i(LOG_TAG,"Scheduling the recording sessions with interval of " + RECORDING_SESSIONS_INTERVAL_MILLIS/1000 + " seconds.");
        _alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                millisWaitBeforeStart,
                RECORDING_SESSIONS_INTERVAL_MILLIS,
                pendingIntent);
    }

    private void startNotificationSchedule() {
        if (_alarmManager == null) {
            Log.e(LOG_TAG,"Alarm manager is null");
            return;
        }

        PendingIntent pendingIntent = createESPendingIntent(ESIntentService.ACTION_NOTIFICATION_CHECKUP);
        // Before registering the repeating alarm, make sure that any similar alarm is canceled:
        _alarmManager.cancel(pendingIntent);

        int notificationIntervalSeconds = ESSettings.notificationIntervalInSeconds();
        long notificationIntervalMillis = 1000*notificationIntervalSeconds;
        Log.i(LOG_TAG,"Scheduling the notification schedule with interval " + notificationIntervalSeconds + " seconds.");
//        _alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                notificationIntervalMillis,
//                notificationIntervalMillis,
//                pendingIntent);
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

    private void stopNotificationSchedule() {
        if (_alarmManager == null) {
            Log.e(LOG_TAG,"Alarm manager is null");
            return;
        }

        PendingIntent pendingIntentReference = createESPendingIntent(ESIntentService.ACTION_NOTIFICATION_CHECKUP);
        _alarmManager.cancel(pendingIntentReference);
        Log.i(LOG_TAG,"Stopped the repeated notification schedule.");
    }

    /**
     * Perform a checkup to see if it's time for user notification.
     * If it's time, trigger the notification.
     */
    public void notificationCheckup() {

        if (!shouldDataCollectionBeOn()) {
            Log.i(LOG_TAG,"Notification: data collection should be off. Not doing notification.");
            turnDataCollectionOff();
        }

        Date now = new Date();
        Date recentTimeAgo = new Date(now.getTime() - RECENT_TIME_PERIOD_IN_MILLIS);
        ESTimestamp lookBackFrom = new ESTimestamp(recentTimeAgo);

        ESActivity latestVerifiedActivity = ESDatabaseAccessor.getESDatabaseAccessor().getLatestVerifiedActivity(lookBackFrom);

        if (latestVerifiedActivity == null) {
            // Then there hasn't been a verified activity in a long time. Need to call for active feedback
            Log.i(LOG_TAG,"Notification: Latest activity was too long ago. Need to prompt for active feedback.");
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.drawable.ic_launcher);
            builder.setContentTitle(NOTIFICATION_TITLE);
            builder.setContentText(NOTIFICATION_TEXT_NO_VERIFIED);

            Intent answerYesIntent = new Intent(this,ESIntentService.class).setAction(ESIntentService.ACTION_LAUNCH_ACTIVE_FEEDBACK);
            PendingIntent answerYesPendingIntent = PendingIntent.getService(this,0,answerYesIntent,0);
            builder.addAction(0,NOTIFICATION_BUTTON_TEXT_YES,answerYesPendingIntent);

            Notification notification = builder.build();
            notification.notify();
        }
        else {
            // Then use this verified activity's labels to ask if still doing the same
            Log.i(LOG_TAG,"Notification: Found latest verified activity. Need to ask user if was doing the same until now.");
            long millisPassed = new Date().getTime() - latestVerifiedActivity.get_timestamp().getDateOfTimestamp().getTime();
            int minutesPassed = (int)(millisPassed / MILLISECONDS_IN_MINUTE);
        }

    }

}
