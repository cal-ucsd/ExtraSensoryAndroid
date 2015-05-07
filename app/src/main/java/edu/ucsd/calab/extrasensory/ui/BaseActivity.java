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

    private static final String LOG_TAG = "[BaseActivity]";
    private static final long RECENT_TIME_PERIOD_IN_MILLIS = 20*ESApplication.MILLISECONDS_IN_MINUTE;
    private static final int NOTIFICATION_ID = 2;
    private static final String NOTIFICATION_TITLE = "ExtraSensory";
    private static final String NOTIFICATION_TEXT_NO_VERIFIED = "Can you please report what you are doing?";
    private static final String ALERT_BUTTON_TEXT_YES = "Yes";
    private static final String ALERT_BUTTON_TEXT_NOT_NOW = "Not now";
    private static final String ALERT_BUTTON_TEXT_CORRECT = "Correct";
    private static final String ALERT_BUTTON_NOT_EXACTLY = "Not exactly";
    private static final String ALERT_TEXT_NO_VERIFIED = NOTIFICATION_TEXT_NO_VERIFIED;

    protected Menu _optionsMenu = null;

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
            if (ESIntentService.ACTION_NOTIFICATION_CHECKUP.equals(intent.getAction())) {
                Log.v(LOG_TAG,"Caught notification checkup broadcast");
                notificationCheckup();
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
        LocalBroadcastManager.getInstance(this).registerReceiver(_broadcastReceiver,new IntentFilter(ESSensorManager.BROADCAST_RECORDING_STATE_CHANGED));
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
                //TODO: start new feedback view for active feedback:

                dialog.dismiss();
            }
        });
        builder.setNegativeButton(ALERT_BUTTON_TEXT_NOT_NOW,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void displayAlertForPastFeedback(int fromVerifiedActivityTimestampSeconds,int toTimestampSeconds,String question) {
        //TODO: if there is already an existing dialog, dismiss it

        ESTimestamp fromTimestamp = new ESTimestamp(fromVerifiedActivityTimestampSeconds);
        ESTimestamp toTimestamp = new ESTimestamp(toTimestampSeconds);

        ESActivity latestVerifiedActivity = ESDatabaseAccessor.getESDatabaseAccessor().getESActivity(fromTimestamp);
        if (latestVerifiedActivity == null) {
            Log.e(LOG_TAG,"Got request for alert, but with timestamp that has no activity: " + fromVerifiedActivityTimestampSeconds);
            return;
        }
        ESContinuousActivity entireRange = ESDatabaseAccessor.getESDatabaseAccessor().getSingleContinuousActivityFromTimeRange(fromTimestamp,toTimestamp);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher).setMessage(question);
        builder.setPositiveButton(ALERT_BUTTON_TEXT_CORRECT,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //TODO: fill the labels of this continuous activity according to the verified activity and perform feedback on the entire continuous activity
                dialog.dismiss();
            }
        });
        builder.setNeutralButton(ALERT_BUTTON_NOT_EXACTLY,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //TODO: open feedback view with this continuous activity
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

        AlertDialog alertDialog = builder.create();
        //TODO: save a reference for this alert dialog in the app
        alertDialog.show();
    }


    /**
     * Perform a checkup to see if it's time for user notification.
     * If it's time, trigger the notification.
     */
    public void notificationCheckup() {

        if (!getTheESApplication().shouldDataCollectionBeOn()) {
            Log.i(LOG_TAG,"Notification: data collection should be off. Not doing notification.");
            getTheESApplication().checkShouldWeCollectDataAndManageAppropriately();
            return;
        }

        Log.i(LOG_TAG,"Notification: checkup.");
        Date now = new Date();
        Date recentTimeAgo = new Date(now.getTime() - RECENT_TIME_PERIOD_IN_MILLIS);
        ESTimestamp lookBackFrom = new ESTimestamp(recentTimeAgo);

        ESActivity latestVerifiedActivity = ESDatabaseAccessor.getESDatabaseAccessor().getLatestVerifiedActivity(lookBackFrom);

        if (latestVerifiedActivity == null) {
            // Then there hasn't been a verified activity in a long time. Need to call for active feedback
            Log.i(LOG_TAG,"Notification: Latest activity was too long ago. Need to prompt for active feedback.");
            if (getTheESApplication().isAppInForeground()) {
                // Don't deal with notifications. Simply present alert dialog:
                displayAlertForActiveFeedback();
                return;
            }

            // Then we're in the background and we need to raise user's attention with notification:
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.drawable.ic_launcher);
            builder.setContentTitle(NOTIFICATION_TITLE);
            builder.setContentText(NOTIFICATION_TEXT_NO_VERIFIED);
            builder.setPriority(Notification.PRIORITY_HIGH);
            builder.setCategory(Notification.CATEGORY_ALARM);

            Intent defaultActionIntent = new Intent(this, FeedbackActivity.class);
            PendingIntent defaultActionPendingIntent = PendingIntent.getActivity(this, 0, defaultActionIntent, 0);
            //TODO: add parameters indicating the source is notification_fresh - add this to the labelSource also....
            builder.setContentIntent(defaultActionPendingIntent);

            Notification notification = builder.build();
            Log.d(LOG_TAG,"Created notification: " + notification);
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID,notification);
        }
        else {
            // Then use this verified activity's labels to ask if still doing the same
            Log.i(LOG_TAG,"Notification: Found latest verified activity. Need to ask user if was doing the same until now.");
            ESTimestamp nowTimestamp = new ESTimestamp(now);
            long millisPassed = now.getTime() - latestVerifiedActivity.get_timestamp().getDateOfTimestamp().getTime();
            int minutesPassed = (int)(millisPassed / ESApplication.MILLISECONDS_IN_MINUTE);
            String question = getAlertQuestion(latestVerifiedActivity,minutesPassed);

            if (getTheESApplication().isAppInForeground()) {
                // No need to use notification. Simply display an alert dialog:
                displayAlertForPastFeedback(latestVerifiedActivity.get_timestamp().get_secondsSinceEpoch(),nowTimestamp.get_secondsSinceEpoch(),question);
                return;
            }

            // Then we're in the background and we need to raise user's attention with notification:
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.drawable.ic_launcher);
            builder.setContentTitle(NOTIFICATION_TITLE);
            builder.setContentText(question);
            builder.setPriority(Notification.PRIORITY_HIGH);
            builder.setCategory(Notification.CATEGORY_ALARM);

            Intent defaultActionIntent = new Intent(this, MainActivity.class);
            defaultActionIntent.putExtra(MainActivity.KEY_LAST_VERIFIED_TIMESTAMP,latestVerifiedActivity.get_timestamp().get_secondsSinceEpoch());
            defaultActionIntent.putExtra(MainActivity.KEY_UNTIL_TIMESTAMP,nowTimestamp.get_secondsSinceEpoch());
            defaultActionIntent.putExtra(MainActivity.KEY_ALERT_QUESTION,question);

            PendingIntent defaultActionPendingIntent = PendingIntent.getActivity(this, 0, defaultActionIntent, 0);
            builder.setContentIntent(defaultActionPendingIntent);

            Notification notification = builder.build();
            Log.d(LOG_TAG,"Created notification: " + notification);
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID,notification);
        }
    }

    private static String getAlertQuestion(ESActivity latestVerifiedActivity,int minutesPassed) {
        String question = "In the past " + minutesPassed + " minutes were still " + latestVerifiedActivity.get_mainActivityUserCorrection();

        String[] secondaries = latestVerifiedActivity.get_secondaryActivities();
        if (secondaries != null && secondaries.length > 0) {
            question += "(" + secondaries[0];
            for (int i = 1; i < secondaries.length; i ++) {
                question += ", " + secondaries[i];
            }
            question += ")";
        }

        String[] moods = latestVerifiedActivity.get_moods();
        if (moods != null && moods.length > 0) {
            question += " and feeling " + moods[0];
            for (int i = 1; i < moods.length; i ++) {
                question += ", " + moods[i];
            }
        }

        question += "?";
        return question;
    }


}
