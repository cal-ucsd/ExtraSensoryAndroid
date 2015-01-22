package edu.ucsd.calab.extrasensory;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import edu.ucsd.calab.extrasensory.data.ESActivity;
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
    private static final long RECORDING_SESSIONS_INTERVAL_MILLIS = 1000*3;

    private static Context _appContext;

    public static Context getTheAppContext() {
        return _appContext;
    }

    private ESSensorManager _sensorManager;
    private AlarmManager _alarmManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(LOG_TAG, "Application being created.");
        _sensorManager = ESSensorManager.getESSensorManager(getApplicationContext());
        _alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        _appContext = getApplicationContext();
        // Start the scheduling of periodic recordings:

    }

    /**
     * Start a repeating schedule of recording sessions (every 1 minute) from now.
     */
    public void startRecordingSchedule() {
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
    public void stopCurrentRecordingAndRecordingSchedule() {
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
