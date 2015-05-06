package edu.ucsd.calab.extrasensory;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.view.animation.TranslateAnimation;

import edu.ucsd.calab.extrasensory.data.ESActivity;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESLabelStruct;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;
import edu.ucsd.calab.extrasensory.ui.FeedbackActivity;


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
    public static final String ACTION_NOTIFICATION_CHECKUP = "edu.ucsd.calab.extrasensory.action.NOTIFICATION_CHECKUP";

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
            ESActivity newActivity = ESDatabaseAccessor.getESDatabaseAccessor().createNewActivity();
            if (newActivity == null) {
                Log.e(LOG_TAG,"Tried to create new activity but got null");
                return;
            }
            ESTimestamp timestamp = newActivity.get_timestamp();
            Log.v(LOG_TAG,"Created new activity record with timestamp: " + timestamp);

            // Check if there are predetermined labels:
            ESLabelStruct predeterminedLabels = ESApplication._predeterminedLabels.getLabels();
            if (predeterminedLabels != null) {
                // Is this the first activity in a sequence initiated by active feedback?
                ESActivity.ESLabelSource labelSource;
                if (ESApplication._predeterminedLabels.is_startedFirstActivityRecording()) {
                    labelSource = ESActivity.ESLabelSource.ES_LABEL_SOURCE_ACTIVE_START;
                    ESApplication._predeterminedLabels.set_startedFirstActivityRecording(false);
                    Log.i(LOG_TAG,"This new activity is the start of user-initiated activity (active feedback).");
                }
                else {
                    labelSource = ESActivity.ESLabelSource.ES_LABEL_SOURCE_ACTIVE_CONTINUE;
                    Log.i(LOG_TAG,"This new activity is the continue of active feedback.");
                }
                // Set the labels for the newly created activity:
                ESDatabaseAccessor.getESDatabaseAccessor().setESActivityValuesAndPossiblySendFeedback(
                        newActivity,labelSource,newActivity.get_mainActivityServerPrediction(),
                        predeterminedLabels._mainActivity,predeterminedLabels._secondaryActivities,
                        predeterminedLabels._moods,false);
                Log.i(LOG_TAG,"Applied predetermined labels to new activity.");
            }
            else {
                Log.i(LOG_TAG,"This new activity has no predetermined labels.");
            }

            // Now start recording:
            ESSensorManager.getESSensorManager().startRecordingSensors(timestamp);
        }
        else if (ACTION_NOTIFICATION_CHECKUP.equals(action)) {
            Log.i(LOG_TAG,"Got intent for notification checkup");
            ((ESApplication)getApplication()).notificationCheckup();
        }
        else {
            Log.e(LOG_TAG,"Got intent for unsupported action: " + action);
        }

    }

}
