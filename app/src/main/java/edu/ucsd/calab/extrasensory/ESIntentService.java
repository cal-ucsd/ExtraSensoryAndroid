package edu.ucsd.calab.extrasensory;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 *
 * This class is to be used to initiate services for the application.
 * These services requests can be handled using alarms that call this class.
 */
public class ESIntentService extends IntentService {

    public static final String LOG_TAG = "[ESIntentService]";

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_START_RECORDING = "edu.ucsd.calab.extrasensory.action.START_RECORDING";

    public ESIntentService() {
        super("ESIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            Log.e(LOG_TAG,"Got null intent.");
            return;
        }

        final String action = intent.getAction();
        if (action == null) {
            Log.e(LOG_TAG,"Got intent with null action.");
            return;
        }

        Log.v(LOG_TAG,"Got intent with action: " + action);
        if (ACTION_START_RECORDING.equals(action)) {
            ESSensorManager.getESSensorManager(getApplicationContext()).startRecordingSensors();
        } else {
            Log.e(LOG_TAG,"Got intent for unsupported action: " + action);
        }

    }

}
