package edu.ucsd.calab.extrasensory.ui;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Date;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.ESIntentService;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESActivity;
import edu.ucsd.calab.extrasensory.data.ESContinuousActivity;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;

/**
 * Created by Yonatan on 2/24/2015.
 */
public class BaseActivity extends ActionBarActivity {

    public static final String KEY_LAST_VERIFIED_TIMESTAMP = "edu.ucsd.calab.extrasensory.extra_key.last_verified_timestamp";
    public static final String KEY_UNTIL_TIMESTAMP = "edu.ucsd.calab.extrasensory.extra_key.until_timestamp";
    public static final String KEY_ALERT_QUESTION = "edu.ucsd.calab.extrasensory.extra_key.alert_question";
    public static final int LAST_VERIFIED_TIMESTAMP_DEFAULT = -1;
    public static final int UNTIL_TIMESTAMP_DEFAULT = -1;

    private static final String LOG_TAG = "[BaseActivity]";
    private static final String ALERT_BUTTON_TEXT_YES = "Yes";
    private static final String ALERT_BUTTON_TEXT_NOT_NOW = "Not now";
    private static final String ALERT_BUTTON_TEXT_CORRECT = "Correct";
    private static final String ALERT_BUTTON_NOT_EXACTLY = "Not exactly";
    private static final String ALERT_TEXT_NO_VERIFIED = ESApplication.NOTIFICATION_TEXT_NO_VERIFIED;


    protected Menu _optionsMenu = null;
    private AlertDialog _theOnlyDialog = null;

    private ESApplication getTheESApplication() {
        return (ESApplication)getApplication();
    }

    private BroadcastReceiver _broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG,"Caught broadcast with action: " + intent.getAction());
            if (intent == null) {
                Log.e(LOG_TAG, "Broadcast receiver caught null intent");
                return;
            }
            if (ESSensorManager.BROADCAST_RECORDING_STATE_CHANGED.equals(intent.getAction())) {
                Log.v(LOG_TAG, "Caught recording state broadcast");
                checkRecordingStateAndSetRedLight();
                return;
            }
            if (ESApplication.ACTION_ALERT_ACTIVE_FEEDBACK.equals(intent.getAction())) {
                Log.v(LOG_TAG,"Caught 'display alert for active feedback' broadcast");
                displayAlertForActiveFeedback();
                return;
            }
            if (ESApplication.ACTION_ALERT_PAST_FEEDBACK.equals(intent.getAction())) {
                Log.v(LOG_TAG,"Caught 'display alert for past feedback' broadcast");
                displayAlertForPastFeedback(intent);
                return;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setIcon(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkRecordingStateAndSetRedLight();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ESSensorManager.BROADCAST_RECORDING_STATE_CHANGED);
        filter.addAction(ESApplication.ACTION_ALERT_ACTIVE_FEEDBACK);
        filter.addAction(ESApplication.ACTION_ALERT_PAST_FEEDBACK);
        LocalBroadcastManager.getInstance(this).registerReceiver(_broadcastReceiver,filter);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(_broadcastReceiver);
        super.onPause();
    }

    protected void checkRecordingStateAndSetRedLight() {
        if (_optionsMenu == null) {
            // Then there is no place to hide/show the red light:
            return;
        }

        MenuItem redLight = _optionsMenu.findItem(R.id.action_red_circle);
        if (redLight == null) {
            Log.i(LOG_TAG,"Red Light item is null");
            return;
        }
        if (ESSensorManager.getESSensorManager().is_recordingRightNow()) {
            redLight.setVisible(true);
            Log.i(LOG_TAG, "Recording now - turning on red light");
        }
        else {
            redLight.setVisible(false);
            Log.i(LOG_TAG,"Not recording - turning off red light");
        }
    }

    private void displayAlertForActiveFeedback() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher).setMessage(ALERT_TEXT_NO_VERIFIED);
        builder.setPositiveButton(ALERT_BUTTON_TEXT_YES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(getApplicationContext(),FeedbackActivity.class);
                FeedbackActivity.setFeedbackParametersBeforeStartingFeedback(new FeedbackActivity.FeedbackParameters());
                startActivity(intent);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(ALERT_BUTTON_TEXT_NOT_NOW,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        if (_theOnlyDialog != null && _theOnlyDialog.isShowing()) {
            _theOnlyDialog.dismiss();
        }
        _theOnlyDialog = builder.create();
        _theOnlyDialog.show();
    }

    public void displayAlertForPastFeedback(Intent intent) {
        int lastVerifiedTimestampSeconds = intent.getIntExtra(KEY_LAST_VERIFIED_TIMESTAMP, LAST_VERIFIED_TIMESTAMP_DEFAULT);
        int untilTimestampSeconds = intent.getIntExtra(KEY_UNTIL_TIMESTAMP, UNTIL_TIMESTAMP_DEFAULT);
        String question = intent.getStringExtra(KEY_ALERT_QUESTION);
        if (lastVerifiedTimestampSeconds == LAST_VERIFIED_TIMESTAMP_DEFAULT || untilTimestampSeconds == UNTIL_TIMESTAMP_DEFAULT || question == null) {
            Log.e(LOG_TAG,"Activity started from notification, but missing info.");
        }
        else {
            displayAlertForPastFeedback(lastVerifiedTimestampSeconds,untilTimestampSeconds,question);
        }

    }

    private void displayAlertForPastFeedback(int fromVerifiedActivityTimestampSeconds,int toTimestampSeconds,String question) {
        ESTimestamp fromTimestamp = new ESTimestamp(fromVerifiedActivityTimestampSeconds);
        ESTimestamp toTimestamp = new ESTimestamp(toTimestampSeconds);

        final ESActivity latestVerifiedActivity = ESDatabaseAccessor.getESDatabaseAccessor().getESActivity(fromTimestamp);
        if (latestVerifiedActivity == null) {
            Log.e(LOG_TAG,"Got request for alert, but with timestamp that has no activity: " + fromVerifiedActivityTimestampSeconds);
            return;
        }
        final ESContinuousActivity entireRange = ESDatabaseAccessor.getESDatabaseAccessor().getSingleContinuousActivityFromTimeRange(fromTimestamp,toTimestamp);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher).setMessage(question);
        builder.setPositiveButton(ALERT_BUTTON_TEXT_CORRECT,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Update the labels (and send feedback) of all the minutes in the range:
                for (ESActivity minuteActivity : entireRange.getMinuteActivities()) {
                    ESDatabaseAccessor.getESDatabaseAccessor().setESActivityValues(
                            minuteActivity,
                            ESActivity.ESLabelSource.ES_LABEL_SOURCE_NOTIFICATION_ANSWER_CORRECT,
                            latestVerifiedActivity.get_mainActivityUserCorrection(),
                            latestVerifiedActivity.get_secondaryActivities(),
                            latestVerifiedActivity.get_moods());
                }
                dialog.dismiss();
            }
        });
        builder.setNeutralButton(ALERT_BUTTON_NOT_EXACTLY,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(getApplicationContext(),FeedbackActivity.class);
                FeedbackActivity.setFeedbackParametersBeforeStartingFeedback(new FeedbackActivity.FeedbackParameters(entireRange));
                startActivity(intent);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(ALERT_BUTTON_TEXT_NOT_NOW,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing. Just close the alert:
                dialog.dismiss();
            }
        });

        if (_theOnlyDialog != null && _theOnlyDialog.isShowing()) {
            _theOnlyDialog.dismiss();
        }
        _theOnlyDialog = builder.create();
        _theOnlyDialog.show();
    }




}
